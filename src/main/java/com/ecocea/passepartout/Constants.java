package com.ecocea.passepartout;

public final class Constants {

    private Constants() {}

    public static final String AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
    public static final String AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";

    //Mail
    public static final String MAIL_TEMPLATE_FOLDER = "mail/";
    public static final String MAIL_TEMPLATE_ALL_KEYS_EXPIRED = "AllKeysExpired.html";
    public static final String MAIL_TEMPLATE_GENERATED_NEW_KEY = "GeneratedNewKey.html";
    public static final String MAIL_TEMPLATE_UPDATE_ERROR = "UpdateErrorTemplate.html";
    public static final String MAIL_TEMPLATE_DELETED_OLD_KEY = "DeletedOldKey.html";
    public static final String MAIL_TEMPLATE_DELETE_CURRENT_KEY = "DeleteCurrentKey.html";


    //Heroku
    public static final String HEROKU_BASE_URL = "https://api.heroku.com/";
    public static final String BEARER = "Bearer ";

    //Gitlab
    public static final String GITLAB_BASE_URL = "https://gitlab.ecoceatechnologies.com//api/v4/";
    public static final String PRIVATE_TOKEN = "PRIVATE-TOKEN";
}
