import java.sql.ResultSet;
import java.sql.PreparedStatement;
                
id         = actionContext.get("id");
conn        = actionContext.get("conn");
action     = actionContext.get("action");
errors     = actionContext.get("errors");
trace      = actionContext.get("trace");
changedAttrs = actionContext.get("changedAttributes");

/* Append an hr_emp_num attribute from database */
StringBuffer sqlCmdBuf = new StringBuffer();
sqlCmdBuf.append("Select employee_number from per_people_f where person_id =");
sqlCmdBuf.append(" (Select employee_id from fnd_user where user_name = ?) ");
String sql = sqlCmdBuf.toString();
PreparedStatement st = null;
ResultSet res = null;
try {
    st = conn.prepareStatement(sql);
    st.setString(1, id);
    res = st.executeQuery();
    if ( res.next() ) {
        String empNum = res.getString("EMPLOYEE_NUMBER");
        changedAttrs.put("hr_emp_num", empNum);
    }
} finally {
    if (res!=null)
        res.close();
    if (st!=null)
        st.close();
}
