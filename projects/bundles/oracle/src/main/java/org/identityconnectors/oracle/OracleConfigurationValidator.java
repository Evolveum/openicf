package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleMessages.*;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.dbcommon.LocalizedAssert;
import org.identityconnectors.oracle.OracleConfiguration.ConnectionType;

/** Helper class that validated {@link OracleConfiguration} */
final class OracleConfigurationValidator {
	private final OracleConfiguration cfg;
	OracleConfigurationValidator(OracleConfiguration cfg){
		this.cfg = cfg;
	}
	
    void validate() {
    	if(StringUtil.isNotBlank(cfg.getSourceType())){
    		validateExplicit();
    	}
    	else{
    		validateImplicit();
    	}
    }
	
	
    private void validateExplicit(){
    	LocalizedAssert la = new LocalizedAssert(cfg.getConnectorMessages(),true);
        la.assertNotBlank(cfg.getCaseSensitivityString(), MSG_CS_DISPLAY);
        cfg.setCSSetup(new OracleCaseSensitivityBuilder(cfg.getConnectorMessages()).parseMap(cfg.getCaseSensitivityString()).build());
        cfg.setExtraAttributesPolicySetup(new ExtraAttributesPolicySetupBuilder(cfg.getConnectorMessages()).parseMap(cfg.getExtraAttributesPolicyString()).build());
        cfg.setNormalizerName(OracleNormalizerName.valueOf(cfg.getNormalizerString()));
    	//Now we map source type directly to connectionType
    	cfg.setConnType(ConnectionType.resolveType(cfg.getSourceType(), cfg.getConnectorMessages()));
    	switch(cfg.getConnType()){
	    	case DATASOURCE : {
	    		la.assertNotBlank(cfg.getDataSource(), MSG_DATASOURCE_DISPLAY);
	    		cfg.setDriverClassName(null);
	    		break;
	    	}
	    	case THIN : {
	            la.assertNotBlank(cfg.getUser(),MSG_USER_DISPLAY);
	            la.assertNotNull(cfg.getPassword(), MSG_PASSWORD_DISPLAY);
	        	la.assertNotBlank(cfg.getHost(), MSG_HOST_DISPLAY);
	        	la.assertNotBlank(cfg.getPort(), MSG_PORT_DISPLAY);
	        	la.assertNotBlank(cfg.getDatabase(), MSG_DATABASE_DISPLAY);
	        	cfg.setDriverClassName(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME);
	        	break;
	    	}
	    	case OCI : {
	            la.assertNotBlank(cfg.getUser(),MSG_USER_DISPLAY);
	            la.assertNotNull(cfg.getPassword(), MSG_PASSWORD_DISPLAY);
	        	la.assertNotBlank(cfg.getDatabase(), MSG_DATABASE_DISPLAY);
	        	cfg.setDriverClassName(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME);
	        	break;
	    	}
	    	case FULL_URL : {
	            la.assertNotBlank(cfg.getUser(),MSG_USER_DISPLAY);
	            la.assertNotNull(cfg.getPassword(), MSG_PASSWORD_DISPLAY);
	    		la.assertNotBlank(cfg.getDriver(), MSG_DRIVER_DISPLAY);
	        	la.assertNotBlank(cfg.getUrl(), MSG_URL_DISPLAY);
	            if(OracleSpecifics.THIN_DRIVER.equals(cfg.getDriver())){
	                cfg.setDriverClassName(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME);
	            }
	            else if(OracleSpecifics.OCI_DRIVER.equals(cfg.getDriver())){
	                cfg.setDriverClassName(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME);
	            }
	            else{
	            	cfg.setDriverClassName(cfg.getDriver());
	            }
	        	break;
	    	}
    	}
    	if(cfg.getDriverClassName() != null){
            try {
                Class.forName(cfg.getDriverClassName());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(cfg.getConnectorMessages().format(MSG_CANNOT_LOAD_DRIVER, null, cfg.getDriverClassName()) ,e);
            }
    	}
    }
    
    private void validateImplicit(){
    	LocalizedAssert la = new LocalizedAssert(cfg.getConnectorMessages(),true);
        la.assertNotBlank(cfg.getCaseSensitivityString(), MSG_CS_DISPLAY);
        cfg.setCSSetup(new OracleCaseSensitivityBuilder(cfg.getConnectorMessages()).parseMap(cfg.getCaseSensitivityString()).build());
        cfg.setExtraAttributesPolicySetup(new ExtraAttributesPolicySetupBuilder(cfg.getConnectorMessages()).parseMap(cfg.getExtraAttributesPolicyString()).build());
        cfg.setNormalizerName(OracleNormalizerName.valueOf(cfg.getNormalizerString()));
        if(StringUtil.isNotBlank(cfg.getDataSource())){
			la.assertBlank(cfg.getHost(), MSG_HOST_DISPLAY);
			la.assertBlank(cfg.getDatabase(),MSG_DATABASE_DISPLAY);
			la.assertBlank(cfg.getDriver(),MSG_DRIVER_DISPLAY);
			la.assertBlank(cfg.getPort(),MSG_PORT_DISPLAY);
			//If user is not blank, then also password must not be blank
			//Most of datasource configuration will not allow to pass user and password when retrieving connection from ds,
			//But for some configuration it is valid to specify user/password and override configuration at application server level
			if ((StringUtil.isNotBlank(cfg.getUser()) && cfg.getPassword() == null)
					|| (StringUtil.isBlank(cfg.getUser()) && cfg.getPassword() != null)) {
				throw new IllegalArgumentException(cfg.getConnectorMessages()
						.format(MSG_USER_AND_PASSWORD_MUST_BE_SET_BOTH_OR_NONE,
								null));
			}
			cfg.setConnType(ConnectionType.DATASOURCE);
        }
        else{
        	la.assertNotBlank(cfg.getDriver(), MSG_DRIVER_DISPLAY);
            if(StringUtil.isNotBlank(cfg.getUrl())){
                la.assertNotBlank(cfg.getUser(),MSG_USER_DISPLAY);
                la.assertNotNull(cfg.getPassword(), MSG_PASSWORD_DISPLAY);
    			la.assertBlank(cfg.getHost(), "MSG_HOST_DISPLAY");
    			la.assertBlank(cfg.getDatabase(),MSG_DATABASE_DISPLAY);
    			la.assertBlank(cfg.getPort(),MSG_PORT_DISPLAY);
                if(OracleSpecifics.THIN_DRIVER.equals(cfg.getDriver())){
                	cfg.setDriverClassName(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME);
                }
                else if(OracleSpecifics.OCI_DRIVER.equals(cfg.getDriver())){
                	cfg.setDriverClassName(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME);
                }
                else{
                	cfg.setDriverClassName(cfg.getDriver());
                }
                cfg.setConnType(ConnectionType.FULL_URL);
            }
            else if(OracleSpecifics.THIN_DRIVER.equals(cfg.getDriver()) || OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME.equals(cfg.getDriver())){
            	la.assertNotBlank(cfg.getHost(), MSG_HOST_DISPLAY);
            	la.assertNotBlank(cfg.getPort(), MSG_PORT_DISPLAY);
            	la.assertNotBlank(cfg.getUser(), MSG_USER_DISPLAY);
            	la.assertNotNull(cfg.getPassword(), MSG_PASSWORD_DISPLAY);
            	la.assertNotBlank(cfg.getDatabase(), MSG_DATABASE_DISPLAY);
                cfg.setDriverClassName(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME);
                cfg.setConnType(ConnectionType.THIN);
            }
            else if(OracleSpecifics.OCI_DRIVER.equals(cfg.getDriver())){
            	la.assertNotBlank(cfg.getUser(), MSG_USER_DISPLAY);
            	la.assertNotNull(cfg.getPassword(), MSG_PASSWORD_DISPLAY);
            	la.assertNotBlank(cfg.getDatabase(), MSG_DATABASE_DISPLAY);
                cfg.setDriverClassName(OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME);
                cfg.setConnType(ConnectionType.OCI);
            }
            else{
            	throw new IllegalArgumentException(cfg.getConnectorMessages().format(MSG_SET_DRIVER_OR_URL,null));
            }
            if(cfg.getDriverClassName() != null){
                try {
                    Class.forName(cfg.getDriverClassName());
                } catch (ClassNotFoundException e) {
                	throw new IllegalArgumentException(cfg.getConnectorMessages().format(MSG_CANNOT_LOAD_DRIVER, null, cfg.getDriverClassName()) ,e);
                }
            }
        }
    }

}
