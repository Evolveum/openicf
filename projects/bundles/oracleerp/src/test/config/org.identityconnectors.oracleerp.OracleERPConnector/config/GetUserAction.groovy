import java.sql.ResultSet;
import java.sql.PreparedStatement;
                
id         = actionContext.get("id");
conn        = actionContext.get("conn");
action     = actionContext.get("action");
errors     = actionContext.get("errors");
trace      = actionContext.get("trace");
changedAttrs = actionContext.get("changedAttributes");

StringBuffer sqlCmdBuf = new StringBuffer();
sqlCmdBuf.append("SELECT  USER_ID FROM fnd_user ");
sqlCmdBuf.append(" where USER_NAME = ?");
String sql = sqlCmdBuf.toString();
PreparedStatement st = null;
ResultSet res = null;
try {
  st = conn.prepareStatement(sql);
  st.setString(1, id);
  res = st.executeQuery();
  if ( res.next() ) {
          String userId = res.getString("USER_ID");
          changedAttrs.put("user_id", userId);
  }
} finally {
  if (res!=null)
    res.close();
  if (st!=null)
    st.close();
}