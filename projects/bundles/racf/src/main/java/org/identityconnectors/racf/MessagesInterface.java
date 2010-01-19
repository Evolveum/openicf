package org.identityconnectors.racf;

/**
 * The Interface going to be used to decouple util classes from configuration
 */
public interface MessagesInterface {

    public abstract String getMessage(String key);

    public abstract String getMessage(String key, Object... objects);
}
