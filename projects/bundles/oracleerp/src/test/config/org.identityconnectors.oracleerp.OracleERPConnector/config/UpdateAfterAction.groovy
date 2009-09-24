import java.sql.ResultSet;
import java.sql.PreparedStatement;
        
id         = actionContext.get("id");
conn       = actionContext.get("conn");
action     = actionContext.get("action");
errors     = actionContext.get("errors");
trace      = actionContext.get("trace");
attrs      = actionContext.get("attributes");
String ret = null;

if (attrs.containsKey("email_address")) {
    String emailAddress = attrs.get("email_address");
    String sql = "UPDATE FND_USER SET email_address = ? where upper(user_name) = upper(?)";
    PreparedStatement st = null;
    ResultSet res = null;
    
    /* Set the email */
    try {
        st = conn.prepareStatement(sql);
        st.setString(1,emailAddress);
        st.setString(2, id);
        st.execute();
        return emailAddress;
    } finally {
        if (st != null) {
           st.close();
        }
    }
    
    /* read the email */
    sql = "SELECT email_address FROM FND_USER WHERE upper(user_name) = upper(?)";
    try {
        st = conn.prepareStatement(sql);
        st.setString(1, id);
        res = st.executeQuery();
        if ( res.next() ) {
            ret = res.getString("email_address");
        }
    } finally {
        if (res!=null)
            res.close();
        if (st!=null)
            st.close();
    }
}
return ret;