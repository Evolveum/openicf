package org.identityconnectors.databasetable.config;

import org.identityconnectors.databasetable.DatabaseTableConfiguration;
import org.identityconnectors.databasetable.DatabaseTableConnector;
import org.identityconnectors.databasetable.mapping.misc.JoinDef;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.identityconnectors.common.logging.Log;

public class SqlHandler {

    private String baseTable;
    private String whereClause;
    private Set<String> columns;
    private String sqlQuery;

    private int whereIndex = -1;

    private final Map<String, String> columnAliasBySource = new LinkedHashMap<>();
    private final Map<String, String> sourceByColumnAlias = new LinkedHashMap<>();
    private final Map<String, String> tableAliases = new LinkedHashMap<>();
    private final List<JoinDef> joins = new ArrayList<>();
    private final Map<String, String> columnSchemaFormat = new LinkedHashMap<>();
    static Log log = Log.getLog(DatabaseTableConnector.class);

    public SqlHandler(DatabaseTableConfiguration config) {
        this.parseSql(config);
    }

    public void parseSql(DatabaseTableConfiguration config) {
        try (BufferedReader reader = new BufferedReader(new FileReader(config.getSqlFilePath()))) {
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sqlBuilder.append(line).append("\n");
            }
            String raw = sqlBuilder.toString();

            setSqlQuery(raw);
            parseFromAndJoins(this.sqlQuery);
            parseSelect(this.sqlQuery);
            setWhereClause(this.sqlQuery);

            log.ok("commit SQL was parsed");
            log.ok("Table aliases are as follows:\n", tableAliases);
            log.ok("Column aliases are as follows:\n", sourceByColumnAlias);
            log.ok("Joins are as follows:\n", joins);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setSqlQuery(String sqlQuery) {
        sqlQuery = sqlQuery.replaceAll("\\s+", " ").trim();
        this.sqlQuery = sqlQuery;
    }

    private void parseSelect(String sql) {
        String up = sql.toUpperCase(Locale.ROOT);
        int sel = up.indexOf("SELECT ");
        int from = up.indexOf(" FROM ");
        if (sel < 0 || from < 0 || from <= sel + 6) {
            throw new RuntimeException("SELECT/FROM segment not found or malformed.");
        }
        String selectPart = sql.substring(sel + 7, from).trim();

        List<String> items = splitSelectItems(selectPart);
        Set<String> rawCols = new LinkedHashSet<>();

        Pattern asAlias = Pattern.compile("(?i)\\s+AS\\s+([\\w\"`\\[\\].]+)\\s*$");
        Pattern spaceAlias = Pattern.compile("(?i)\\s+([\\w\"`\\[\\].]+)\\s*$");

        for (String item : items) {
            String trimmed = item.trim();
            String sourceExpr = trimmed;
            String alias = null;

            Matcher mAs = asAlias.matcher(trimmed);
            if (mAs.find()) {
                alias = stripQuotes(mAs.group(1));
                sourceExpr = trimmed.substring(0, mAs.start()).trim();
            } else {
                Matcher mSp = spaceAlias.matcher(trimmed);
                if (mSp.find()) {
                    String last = stripQuotes(mSp.group(1));
                    if (!last.equalsIgnoreCase("DESC")
                            && !last.equalsIgnoreCase("ASC")
                            && !last.equalsIgnoreCase("NULLS")
                            && !last.equalsIgnoreCase("FIRST")
                            && !last.equalsIgnoreCase("LAST")) {
                        alias = last;
                        sourceExpr = trimmed.substring(0, mSp.start()).trim();
                    }
                }
            }

            rawCols.add(sourceExpr);

            String resolvedForSchema = guessSourceColumn(sourceExpr);
            if (resolvedForSchema == null) {
                resolvedForSchema = extractPrimaryColumnFromExpr(sourceExpr);
            }

            if (alias != null) {
                columnAliasBySource.put(sourceExpr, alias);
                sourceByColumnAlias.put(alias, sourceExpr);

                if (resolvedForSchema != null) {
                    columnSchemaFormat.put(alias, resolvedForSchema);
                }
            } else {
                if (resolvedForSchema != null) {
                    columnSchemaFormat.put(resolvedForSchema, resolvedForSchema);
                }
            }
        }

        this.columns = rawCols;

        if (!columnSchemaFormat.isEmpty()) {
            log.ok("Column schema format (alias -> table.column):\n", columnSchemaFormat);
        }
    }

    private String extractPrimaryColumnFromExpr(String expr) {
        if (expr == null || expr.isEmpty()) {
            return null;
        }

        String noStrings = stripStringLiterals(expr);

        Pattern colRef = Pattern.compile("(?i)(?:^|[^A-Za-z0-9_])([A-Za-z0-9_\"`\\[\\]]+)\\.([A-Za-z0-9_\"`\\[\\]]+)(?:[^A-Za-z0-9_]|$)");
        Matcher m = colRef.matcher(noStrings);
        while (m.find()) {
            String qualifierRaw = stripQuotes(m.group(1));
            String columnRaw = stripQuotes(m.group(2));
            String qualified = resolveQualified(qualifierRaw, columnRaw);
            if (qualified != null) {
                return qualified;
            }
        }

        return null;
    }

    private String resolveQualified(String qualifier, String column) {
        if (column == null || column.isEmpty()) {
            return null;
        }
        String qual = (qualifier != null) ? qualifier.trim() : "";
        String col = column.trim();

        int qdot = qual.lastIndexOf('.');
        String lastSegment = (qdot >= 0) ? qual.substring(qdot + 1).trim() : qual;

        String mapped = this.tableAliases.get(lastSegment);
        if (mapped != null && !mapped.isEmpty()) {
            return mapped + "." + col;
        }

        if (!qual.isEmpty()) {
            return qual + "." + col;
        } else {
            return col;
        }
    }

    private String stripStringLiterals(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (!inDouble && c == '\'') {
                if (inSingle) {
                    char next = (i + 1 < s.length()) ? s.charAt(i + 1) : '\0';
                    if (next == '\'') {
                        i++;
                        out.append(' ');
                        out.append(' ');
                        continue;
                    } else {
                        inSingle = false;
                        out.append(' ');
                        continue;
                    }
                } else {
                    inSingle = true;
                    out.append(' ');
                    continue;
                }
            }

            if (!inSingle && c == '\"') {
                if (inDouble) {
                    char next = (i + 1 < s.length()) ? s.charAt(i + 1) : '\0';
                    if (next == '\"') {
                        i++;
                        out.append(' ');
                        out.append(' ');
                        continue;
                    } else {
                        inDouble = false;
                        out.append(' ');
                        continue;
                    }
                } else {
                    inDouble = true;
                    out.append(' ');
                    continue;
                }
            }

            if (inSingle || inDouble) {
                out.append(' ');
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }


    private void parseFromAndJoins(String sql) {
        String up = sql.toUpperCase(Locale.ROOT);

        int fromIdx = up.indexOf(" FROM ");
        if (fromIdx < 0) {
            throw new RuntimeException("FROM keyword does not exist.");
        }

        int end = findFirstOf(up, List.of(" WHERE ", " GROUP BY ", " ORDER BY ", ";"), fromIdx + 6);
        if (end < 0) {
            end = sql.length();
        }

        String fromPart = sql.substring(fromIdx + 6, end).trim();

        int joinPos = indexOfJoin(fromPart.toUpperCase(Locale.ROOT));
        String firstTableChunk = (joinPos >= 0) ? fromPart.substring(0, joinPos).trim() : fromPart;

        String[] toks = firstTableChunk.split("\\s+");
        if (toks.length >= 1) {
            String t0 = stripComma(toks[0]);
            String realTable = stripQuotes(t0);
            String alias = null;
            if (toks.length >= 3 && toks[1].equalsIgnoreCase("AS")) {
                alias = stripQuotes(stripComma(toks[2]));
            } else if (toks.length >= 2 && !toks[1].equalsIgnoreCase("AS")) {
                alias = stripQuotes(stripComma(toks[1]));
            }

            this.baseTable = realTable;
            if (alias != null) {
                tableAliases.put(alias, realTable);
            } else {
                tableAliases.put(realTable, realTable);
            }
        } else {
            throw new RuntimeException("Cannot parse base table from FROM clause.");
        }

        if (joinPos >= 0) {
            String joinSection = fromPart.substring(joinPos).trim();
            parseJoins(joinSection);
        }
    }

    private void parseJoins(String joinSection) {
        Pattern p = Pattern.compile(
                "(?i)\\b((LEFT|RIGHT|INNER|FULL|CROSS)\\s+JOIN|JOIN)\\s+([^\\s]+)(?:\\s+(?:AS\\s+)?(\\w+))?\\s+ON\\s+(.+?)(?=(\\s+(?:LEFT|RIGHT|INNER|FULL|CROSS)\\s+JOIN|\\s+JOIN|\\s+WHERE|\\s+GROUP\\s+BY|\\s+ORDER\\s+BY|;|$))"
        );
        Matcher m = p.matcher(joinSection);
        while (m.find()) {
            String joinType = m.group(1).trim();
            String tbl = stripQuotes(m.group(3).trim());
            String alias = m.group(4) != null ? stripQuotes(m.group(4).trim()) : null;
            String on = m.group(5).trim();

            if (alias != null) {
                tableAliases.put(alias, tbl);
            }
            tableAliases.put(tbl, tbl);

            joins.add(new JoinDef(joinType.toUpperCase(Locale.ROOT), tbl, alias, on));
        }
    }

    private String guessSourceColumn(String expr) {
        String e = expr.trim();
        if (e.startsWith("(") && e.endsWith(")")) {
            e = e.substring(1, e.length() - 1).trim();
        }
        if (e.matches(".*\\s+.*")) {
            return null;
        }
        if (e.contains("(") || e.contains(")")) {
            return null;
        }

        int dot = e.lastIndexOf('.');
        if (dot < 0) {
            return e;
        }

        String qualifier = e.substring(0, dot).trim();
        String column = e.substring(dot + 1).trim();

        int qdot = qualifier.lastIndexOf('.');
        String lastSegment = (qdot >= 0) ? qualifier.substring(qdot + 1).trim() : qualifier;

        String mapped = this.tableAliases.get(lastSegment);
        if (mapped != null && !mapped.isEmpty() && !mapped.equals(lastSegment)) {
            if (qdot >= 0) {
                qualifier = qualifier.substring(0, qdot + 1) + mapped;
            } else {
                qualifier = mapped;
            }
        }

        return qualifier + "." + column;
    }

    private List<String> splitSelectItems(String selectPart) {
        List<String> res = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int paren = 0;
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < selectPart.length(); i++) {
            char c = selectPart.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                cur.append(c);
                continue;
            }
            if (c == '\"' && !inSingle) {
                inDouble = !inDouble;
                cur.append(c);
                continue;
            }
            if (!inSingle && !inDouble) {
                if (c == '(') {
                    paren++;
                } else if (c == ')') {
                    paren--;
                } else if (c == ',' && paren == 0) {
                    res.add(cur.toString());
                    cur.setLength(0);
                    continue;
                }
            }
            cur.append(c);
        }
        if (cur.length() > 0) {
            res.add(cur.toString());
        }
        return res;
    }

    private int findFirstOf(String haystackUp, List<String> needlesUp, int fromIdx) {
        int best = -1;
        for (String n : needlesUp) {
            int idx = haystackUp.indexOf(n, fromIdx);
            if (idx >= 0 && (best < 0 || idx < best)) {
                best = idx;
            }
        }
        return best;
    }

    private int indexOfJoin(String fromUp) {
        Pattern p = Pattern.compile("\\b(LEFT|RIGHT|INNER|FULL|CROSS)\\s+JOIN\\b|\\bJOIN\\b");
        Matcher m = p.matcher(fromUp);
        if (m.find()) {
            return m.start();
        }
        return -1;
    }

    private String stripQuotes(String s) {
        if (s == null) {
            return null;
        }
        String x = s.trim();
        if ((x.startsWith("\"") && x.endsWith("\"")) ||
                (x.startsWith("`") && x.endsWith("`")) ||
                (x.startsWith("[") && x.endsWith("]"))) {
            return x.substring(1, x.length() - 1);
        }
        return x;
    }

    private String stripComma(String s) {
        if (s == null) {
            return null;
        }
        String x = s.trim();
        if (x.endsWith(",")) {
            return x.substring(0, x.length() - 1);
        }
        return x;
    }

    private void setWhereClause(String sql) {
        if (sql == null || sql.isEmpty()) {
            this.whereIndex = -1;
            this.whereClause = null;
            return;
        }

        int lastFrom = findLastTopLevelKeyword(sql, "FROM");
        if (lastFrom < 0) {
            this.whereIndex = -1;
            this.whereClause = null;
            return;
        }

        int wIdx = findFirstTopLevelKeywordFrom(sql, "WHERE", lastFrom + 4);
        if (wIdx >= 0) {
            this.whereIndex = wIdx;

            int end = findFirstOfTopLevelClauses(sql, wIdx + 5);
            if (end < 0) {
                end = sql.length();
            }

            int contentStart = wIdx + "WHERE".length();
            if (contentStart < sql.length() && Character.isWhitespace(sql.charAt(contentStart))) {
                contentStart++;
            }
            this.whereClause = sql.substring(contentStart, end).trim();
            return;
        }

        int tail = findFirstOfTopLevelClauses(sql, lastFrom + 4);
        if (tail < 0) {
            tail = sql.length();
        }
        this.whereIndex = tail;
        this.whereClause = null;
    }

    private static int findLastTopLevelKeyword(String sql, String keyword) {
        String up = sql.toUpperCase(Locale.ROOT);
        String kw = keyword.toUpperCase(Locale.ROOT);

        int depth = 0;
        Character inQuote = null;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        int lastIdx = -1;

        for (int i = 0; i < up.length(); i++) {
            char c = up.charAt(i);
            char n = (i + 1 < up.length()) ? up.charAt(i + 1) : '\0';

            // komentáre
            if (inQuote == null) {
                if (!inBlockComment && !inLineComment && c == '-' && n == '-') {
                    inLineComment = true; i++;
                    continue;
                }
                if (!inBlockComment && !inLineComment && c == '/' && n == '*') {
                    inBlockComment = true; i++;
                    continue;
                }
                if (inLineComment && (c == '\n' || c == '\r')) {
                    inLineComment = false;
                    continue;
                }
                if (inBlockComment && c == '*' && n == '/') {
                    inBlockComment = false; i++;
                    continue;
                }
                if (inLineComment || inBlockComment) {
                    continue;
                }
            }

            // stringy / identifikátory
            if (!inLineComment && !inBlockComment) {
                if (inQuote != null) {
                    if (c == inQuote) {
                        char next = (i + 1 < up.length()) ? up.charAt(i + 1) : '\0';
                        if (next == inQuote) {
                            i++;
                            continue;
                        }
                        inQuote = null;
                    }
                    continue;
                } else {
                    if (c == '\'' || c == '"' || c == '`') {
                        inQuote = c;
                        continue;
                    }
                }
            }

            // hĺbka zátvoriek
            if (c == '(') {
                depth++;
                continue;
            }
            if (c == ')') {
                if (depth > 0) {
                    depth--;
                }
                continue;
            }

            // match iba na depth==0 a hraniciach slova
            if (depth == 0) {
                int m = kw.length();
                if (i + m <= up.length() && up.regionMatches(i, kw, 0, m)) {
                    boolean leftOk = (i == 0) || (!Character.isLetterOrDigit(up.charAt(i - 1)) && up.charAt(i - 1) != '_');
                    boolean rightOk = (i + m == up.length()) || (!Character.isLetterOrDigit(up.charAt(i + m)) && up.charAt(i + m) != '_');
                    if (leftOk && rightOk) {
                        lastIdx = i;
                    }
                }
            }
        }
        return lastIdx;
    }

    private static int findFirstTopLevelKeywordFrom(String sql, String keyword, int startIndex) {
        String up = sql.toUpperCase(Locale.ROOT);
        String kw = keyword.toUpperCase(Locale.ROOT);

        int depth = 0;
        Character inQuote = null;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = Math.max(0, startIndex); i < up.length(); i++) {
            char c = up.charAt(i);
            char n = (i + 1 < up.length()) ? up.charAt(i + 1) : '\0';

            // komentáre
            if (inQuote == null) {
                if (!inBlockComment && !inLineComment && c == '-' && n == '-') {
                    inLineComment = true; i++;
                    continue;
                }
                if (!inBlockComment && !inLineComment && c == '/' && n == '*') {
                    inBlockComment = true; i++;
                    continue;
                }
                if (inLineComment && (c == '\n' || c == '\r')) {
                    inLineComment = false;
                    continue;
                }
                if (inBlockComment && c == '*' && n == '/') {
                    inBlockComment = false; i++;
                    continue;
                }
                if (inLineComment || inBlockComment) {
                    continue;
                }
            }

            // stringy / identifikátory
            if (!inLineComment && !inBlockComment) {
                if (inQuote != null) {
                    if (c == inQuote) {
                        char next = (i + 1 < up.length()) ? up.charAt(i + 1) : '\0';
                        if (next == inQuote) {
                            i++;
                            continue;
                        }
                        inQuote = null;
                    }
                    continue;
                } else {
                    if (c == '\'' || c == '"' || c == '`') {
                        inQuote = c;
                        continue;
                    }
                }
            }

            // hĺbka
            if (c == '(') {
                depth++;
                continue;
            }
            if (c == ')') {
                if (depth > 0) {
                    depth--;
                }
                continue;
            }

            if (depth == 0) {
                int m = kw.length();
                if (i + m <= up.length() && up.regionMatches(i, kw, 0, m)) {
                    boolean leftOk = (i == 0) || (!Character.isLetterOrDigit(up.charAt(i - 1)) && up.charAt(i - 1) != '_');
                    boolean rightOk = (i + m == up.length()) || (!Character.isLetterOrDigit(up.charAt(i + m)) && up.charAt(i + m) != '_');
                    if (leftOk && rightOk) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private static int findFirstOfTopLevelClauses(String sql, int startIndex) {
        String up = sql.toUpperCase(Locale.ROOT);
        String[] clauses = new String[] { "GROUP BY", "HAVING", "ORDER BY", "LIMIT", "OFFSET", "FETCH" };

        int depth = 0;
        Character inQuote = null;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = Math.max(0, startIndex); i < up.length(); i++) {
            char c = up.charAt(i);
            char n = (i + 1 < up.length()) ? up.charAt(i + 1) : '\0';

            // komentáre
            if (inQuote == null) {
                if (!inBlockComment && !inLineComment && c == '-' && n == '-') {
                    inLineComment = true; i++;
                    continue;
                }
                if (!inBlockComment && !inLineComment && c == '/' && n == '*') {
                    inBlockComment = true; i++;
                    continue;
                }
                if (inLineComment && (c == '\n' || c == '\r')) {
                    inLineComment = false;
                    continue;
                }
                if (inBlockComment && c == '*' && n == '/') {
                    inBlockComment = false; i++;
                    continue;
                }
                if (inLineComment || inBlockComment) {
                    continue;
                }
            }

            // stringy / identifikátory
            if (!inLineComment && !inBlockComment) {
                if (inQuote != null) {
                    if (c == inQuote) {
                        char next = (i + 1 < up.length()) ? up.charAt(i + 1) : '\0';
                        if (next == inQuote) {
                            i++;
                            continue;
                        }
                        inQuote = null;
                    }
                    continue;
                } else {
                    if (c == '\'' || c == '"' || c == '`') {
                        inQuote = c;
                        continue;
                    }
                }
            }

            // hĺbka
            if (c == '(') {
                depth++;
                continue;
            }
            if (c == ')') {
                if (depth > 0) {
                    depth--;
                }
                continue;
            }

            if (depth != 0) {
                continue;
            }

            for (String clause : clauses) {
                int m = clause.length();
                if (i + m <= up.length() && up.regionMatches(i, clause, 0, m)) {
                    boolean leftOk = (i == 0) || (!Character.isLetterOrDigit(up.charAt(i - 1)) && up.charAt(i - 1) != '_');
                    boolean rightOk = (i + m == up.length()) || (!Character.isLetterOrDigit(up.charAt(i + m)) && up.charAt(i + m) != '_');
                    if (leftOk && rightOk) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public String getSqlQuery() {
        return this.sqlQuery;
    }

    public String getTable() {
        return this.baseTable;
    }

    public Set<String> getColumns() {
        return this.columns;
    }

    public String getWhereClause() {
        return this.whereClause;
    }

    public int getWhereIndex() {
        return this.whereIndex;
    }

    public Map<String, String> getColumnAliasBySource() {
        return Collections.unmodifiableMap(columnAliasBySource);
    }

    public Map<String, String> getSourceByColumnAlias() {
        return Collections.unmodifiableMap(sourceByColumnAlias);
    }

    public Map<String, String> getTableAliases() {
        return Collections.unmodifiableMap(tableAliases);
    }

    public List<JoinDef> getJoins() {
        return Collections.unmodifiableList(joins);
    }

    public Map<String, String> getColumnSchemaFormat() {
        return Collections.unmodifiableMap(columnSchemaFormat);
    }
}
