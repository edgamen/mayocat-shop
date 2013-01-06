package org.mayocat.shop.store.rdbms.dbi.dao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jodah.typetools.TypeResolver;
import org.mayocat.shop.model.LocalizedEntity;
import org.mayocat.shop.model.Tenant;
import org.mayocat.shop.model.Translations;
import org.mayocat.shop.store.rdbms.dbi.dao.jointype.EntityFullJoinRow;
import org.mayocat.shop.store.rdbms.dbi.mapper.EntityFullJoinRowMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

public abstract class AbstractLocalizedEntityDAO<E extends LocalizedEntity> implements TranslationDAO,
    EntityDAO<E>
{

    public void insertTranslations(Long entityId, Translations translations)
    {
        List<Long> ids = new ArrayList<Long>();
        List<String> langs = new ArrayList<String>();
        List<String> texts = new ArrayList<String>();

        for (String field : translations.keySet()) {
            Map<Locale, String> fieldTranslations = translations.get(field);
            Long id = createTranslation(entityId, field);
            for (Locale locale : fieldTranslations.keySet()) {
                ids.add(id);
                langs.add(locale.toString());
                texts.add(fieldTranslations.get(locale));
            }
        }

        if (ids.size() > 0) {
            insertTranslations("small", ids, langs, texts);
        }
    }

    public E findBySlugWithTranslations(String type, String slug, Tenant tenant) {
        List<EntityFullJoinRow> rows = this.findBySlugWithTranslationsRows(type, slug, tenant);

        E entity = null;
        Class< ? > thisEntityType = TypeResolver.resolveArguments(getClass(), AbstractLocalizedEntityDAO.class)[0];
        Translations translations = new Translations();
        for (EntityFullJoinRow row : rows) {
            if (entity == null) {
                entity = this.getEntity(row.getEntityData(), thisEntityType);
            }
            String field = row.getField();
            if (field != null) {
                if (!translations.containsKey(field)) {
                    translations.put(field, new HashMap<Locale, String>());
                }
                Map<Locale, String> fieldTranslations = translations.get(field);
                fieldTranslations.put(row.getLocale(), row.getText());
            }
        }
        if (entity != null) {
            entity.setTranslations(translations);
        }
        return entity;
    }

    private E getEntity(Map<String, Object> entityData, Class< ? > type)
    {
        E entity;
        try {
            entity = (E) type.newInstance();

            for (Method method : entity.getClass().getMethods()) {
                if (method.getName().startsWith("set") && !method.getName().equals("setTranslations")
                    && Character.isUpperCase(method.getName().charAt(3))) {
                    // Found a setter.
                    String field = method.getName().substring(3);

                    if (entityData.containsKey(field)) {
                        boolean setterAccessible = method.isAccessible();
                        method.setAccessible(true);
                        method.invoke(entity, entityData.get(field));
                        method.setAccessible(setterAccessible);
                    }
                }
            }
            return entity;

        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (SecurityException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }

    }
    
    @RegisterMapper(EntityFullJoinRowMapper.class)
    @SqlQuery
    (
        "SELECT * " 
      + "FROM   entity " 
      + "       INNER JOIN <type> " 
      + "               ON entity.id = <type>.entity_id " 
      + "       LEFT JOIN translation "
      + "              ON translation.entity_id = entity.id " 
      + "       LEFT JOIN translation_small "
      + "              ON translation_small.translation_id = translation.id " 
      + "       LEFT JOIN translation_medium "
      + "              ON translation_medium.translation_id = translation.id " 
      + "WHERE  entity.slug = :slug " 
      + "       AND entity.type = '<type>' " 
      + "       AND entity.tenant_id = :tenant.id "
    )
    abstract List<EntityFullJoinRow> findBySlugWithTranslationsRows(@Define("type") String type, @Bind("slug") String slug, @BindBean("tenant") Tenant tenant);
}