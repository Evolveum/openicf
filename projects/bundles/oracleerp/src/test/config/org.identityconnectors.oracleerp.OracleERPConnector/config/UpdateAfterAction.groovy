import java.sql.ResultSet;
import java.sql.PreparedStatement;
        
id         = actionContext.get("id");
conn       = actionContext.get("conn");
action     = actionContext.get("action");
errors     = actionContext.get("errors");
trace      = actionContext.get("trace");
attrs      = actionContext.get("attributes");

if (attrs.containsKey("email_address")) {
    String emailAddress = attrs.get("email_address");
    String sql = "UPDATE FND_USER SET email_address = ? where user_name = ?";
    PreparedStatement st = null;
    try {
        st = conn.prepareStatement(sql);
        st.setString(1,emailAddress);
        st.setString(2, id);
        st.execute();
        conn.commit();
    }
    finally {
        if (st != null) {
           st.close();
        }
    }
}