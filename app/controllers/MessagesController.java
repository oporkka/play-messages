package controllers;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import play.Play;
import play.i18n.Messages;
import play.modules.messages.MessagesResource;
import play.modules.messages.MessagesUtil;
import play.modules.messages.SourceKeys;
import play.mvc.Before;
import play.mvc.Controller;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author huljas
 */
public class MessagesController extends Controller {

    @Before
    public static void disableInProduction() {
        if (Play.mode == Play.Mode.PROD) {
            error(404, "Page not found");
        }
    }

    public static void index(String language, String defaultLanguage) {
        if (StringUtils.isBlank(defaultLanguage)) {
            defaultLanguage = Play.langs.get(0);
        }
        if (StringUtils.isBlank(language)) {
            language = defaultLanguage;
        }
        MessagesResource messagesResource = MessagesResource.instance();
        Map<String,String> values = messagesResource.loadMessages(language);
        Map<String,String> defaultValues = messagesResource.loadMessages(defaultLanguage);
        List<String> keepList = messagesResource.loadKeepList();
        List<String> ignoreList = messagesResource.loadIgnoreList();

        SourceKeys sources = SourceKeys.lookUp();

        Collection<String> newKeys = MessagesUtil.getNewKeys(sources, values);
        Collection<String> obsoleteKeys = MessagesUtil.getObsoleteKeys(sources, values);
        Collection<String> existingKeys = MessagesUtil.getExistingKeys(sources, values);

        for (String key : keepList) {
            if (obsoleteKeys.contains(key) || existingKeys.contains(key)) {
                existingKeys.add(key);
            } else {
                newKeys.add(key);
            }
        }
        obsoleteKeys.removeAll(keepList);
        newKeys.removeAll(ignoreList);

        render(language, defaultLanguage, values, defaultValues, sources, newKeys, existingKeys, obsoleteKeys, keepList, ignoreList);
    }

    public static void save(String language, String defaultLanguage, Map<String,String> values, List<String> ignoreList, List<String> removeList, List<String> keepList) throws IOException {
        ignoreList = removeDuplicates(ignoreList);
        removeList = removeDuplicates(removeList);
        keepList = removeDuplicates(keepList);
        MessagesResource messagesResource = MessagesResource.instance();
        Map<String,String> map = messagesResource.loadMessages(language);

        for (Map.Entry<String,String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!StringUtils.isBlank(value)) {
                map.put(key, value);
            }
        }
        for (String key : ignoreList) {
            map.remove(key);
        }
        for (String key : removeList) {
            map.remove(key);
        }
        messagesResource.saveMessages(language, map, removeList);
        messagesResource.saveIgnoreList(ignoreList);
        messagesResource.saveKeepList(keepList);

        flash.success("Messages saved successfully!");

        index(language, defaultLanguage);
    }

    public static void ajaxSave(String language, String key, String value, boolean keep) {
        if (!StringUtils.isBlank(value) && !StringUtils.isBlank(key)) {
            MessagesResource messagesResource = MessagesResource.instance();
            Map<String,String> map = messagesResource.loadMessages(language);
            map.put(key, value);
            messagesResource.saveMessages(language, map, Collections.EMPTY_LIST);
            List<String> keepList = messagesResource.loadKeepList();
            if (keep) {
                if (!keepList.contains(key)) {
                    keepList.add(key);
                    messagesResource.saveKeepList(keepList);
                }
            } else {
                if (keepList.contains(key)) {
                    keepList.remove(key);
                    messagesResource.saveKeepList(keepList);
                }
            }
        }
        render(value);
    }

    public static void applyChanges(String language, String defaultLanguage, MessagesAction action, List<String> keys) {
        if (action == MessagesAction.DELETE) {
            MessagesResource messagesResource = MessagesResource.instance();
            Map<String,String> map = messagesResource.loadMessages(language);
            messagesResource.saveMessages(language, map, keys);
        } else if (action == MessagesAction.IGNORE) {
            MessagesResource messagesResource = MessagesResource.instance();
            List<String> ignoreList = messagesResource.loadIgnoreList();
            ignoreList.removeAll(keys);
            ignoreList.addAll(keys);
            messagesResource.saveIgnoreList(ignoreList);
        } else if (action == MessagesAction.UNIGNORE) {
            MessagesResource messagesResource = MessagesResource.instance();
            List<String> ignoreList = messagesResource.loadIgnoreList();
            ignoreList.removeAll(keys);
            messagesResource.saveIgnoreList(ignoreList);
        }
        index(language, defaultLanguage);
    }

    public static void addKey(String language, String defaultLanguage, String key) {
        MessagesResource messagesResource = MessagesResource.instance();
        Map<String,String> localizations = messagesResource.loadMessages(language);
        Map<String,String> defaultLocalizations = messagesResource.loadMessages(defaultLanguage);
        List<String> keepList = new ArrayList<String>();
        keepList.add(key);
        render("_row.html", localizations, defaultLocalizations, key, keepList);
    }

    public static void sources(String key) {
        SourceKeys sources = SourceKeys.lookUp();
        render(key, sources);
    }

    private static List<String> removeDuplicates(List<String> list) {
        if (list == null) {
            return Collections.EMPTY_LIST;
        }
        return new ArrayList<String>(new HashSet<String>(list));
    }
}
