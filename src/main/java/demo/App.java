/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package demo;

import org.apache.kafka.common.cache.Cache;
import org.apache.kafka.common.cache.LRUCache;
import org.apache.kafka.common.cache.SynchronizedCache;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.common.utils.ConfigUtils;
import org.apache.kafka.connect.components.Versioned;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.SchemaUtil;
import org.apache.kafka.connect.transforms.util.SimpleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.kafka.connect.transforms.util.Requirements.requireMap;
import static org.apache.kafka.connect.transforms.util.Requirements.requireStruct;

public abstract class App<R extends ConnectRecord<R>> implements Transformation<R>, Versioned {

    private static final Logger log = LoggerFactory.getLogger(App.class);
    public static final String OVERVIEW_DOC = "Filter or rename fields."
            + "<p/>Use the concrete transformation type designed for the record key (<code>" + Key.class.getName() + "</code>) "
            + "or value (<code>" + Value.class.getName() + "</code>).";
    public static final String BEFORE = "before";
    public static final String AFTER = "after";

    interface ConfigName {
        String EXCLUDE = "exclude";
        String INCLUDE = "include";

        // for backwards compatibility
        String INCLUDE_ALIAS = "whitelist";
        String EXCLUDE_ALIAS = "blacklist";

        String RENAME = "renames";
    }

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(ConfigName.EXCLUDE, ConfigDef.Type.LIST, Collections.emptyList(), ConfigDef.Importance.MEDIUM,
                    "Fields to exclude. This takes precedence over the fields to include.")
            .define("blacklist", ConfigDef.Type.LIST, null, Importance.LOW,
                    "Deprecated. Use " + ConfigName.EXCLUDE + " instead.")
            .define(ConfigName.INCLUDE, ConfigDef.Type.LIST, Collections.emptyList(), ConfigDef.Importance.MEDIUM,
                    "Fields to include. If specified, only these fields will be used.")
            .define("whitelist", ConfigDef.Type.LIST, null, Importance.LOW,
                    "Deprecated. Use " + ConfigName.INCLUDE + " instead.")
            .define(ConfigName.RENAME, ConfigDef.Type.LIST, Collections.emptyList(), new ConfigDef.Validator() {
                @SuppressWarnings("unchecked")
                @Override
                public void ensureValid(String name, Object value) {
                    parseRenameMappings((List<String>) value);
                }

                @Override
                public String toString() {
                    return "list of colon-delimited pairs, e.g. <code>foo:bar,abc:xyz</code>";
                }
            }, ConfigDef.Importance.MEDIUM, "Field rename mappings.");

    private static final String PURPOSE = "field replacement";

    private Set<String> exclude;
    private Set<String> include;
    private Map<String, String> renames;
    private Map<String, String> reverseRenames;

    private Cache<Schema, Schema> schemaUpdateCache;

    @Override
    public String version() {
        return AppInfoParser.getVersion();
    }

    @Override
    public void configure(Map<String, ?> configs) {
        final SimpleConfig config = new SimpleConfig(CONFIG_DEF, ConfigUtils.translateDeprecatedConfigs(configs, new String[][]{
                {ConfigName.INCLUDE, "whitelist"},
                {ConfigName.EXCLUDE, "blacklist"},
        }));

        exclude = new HashSet<>(config.getList(ConfigName.EXCLUDE));
        include = new HashSet<>(config.getList(ConfigName.INCLUDE));
        renames = parseRenameMappings(config.getList(ConfigName.RENAME));
        reverseRenames = invert(renames);

        schemaUpdateCache = new SynchronizedCache<>(new LRUCache<>(16));
    }

    static Map<String, String> parseRenameMappings(List<String> mappings) {
        final Map<String, String> m = new HashMap<>();
        for (String mapping : mappings) {
            final String[] parts = mapping.split(":");
            if (parts.length != 2) {
                throw new ConfigException(ConfigName.RENAME, mappings, "Invalid rename mapping: " + mapping);
            }
            m.put(parts[0], parts[1]);
        }
        return m;
    }

    static Map<String, String> invert(Map<String, String> source) {
        final Map<String, String> m = new HashMap<>();
        for (Map.Entry<String, String> e : source.entrySet()) {
            m.put(e.getValue(), e.getKey());
        }
        return m;
    }

    @Override
    public R apply(R record) {
        log.info("##log record: {}", record);
        if (record.value() == null) {
            log.info("##log tombstone record");
            return null;
        }
        log.info("##log normal record");
        return record;
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public void close() {
        schemaUpdateCache = null;
    }

    protected abstract Schema operatingSchema(R record);

    protected abstract Object operatingValue(R record);

    protected abstract R newRecord(R record, Schema updatedSchema, Object updatedValue);

    public static class Key<R extends ConnectRecord<R>> extends App<R> {

        @Override
        protected Schema operatingSchema(R record) {
            return record.keySchema();
        }

        @Override
        protected Object operatingValue(R record) {
            return record.key();
        }

        @Override
        protected R newRecord(R record, Schema updatedSchema, Object updatedValue) {
            return record.newRecord(record.topic(), record.kafkaPartition(), updatedSchema, updatedValue, record.valueSchema(), record.value(), record.timestamp());
        }

    }

    public static class Value<R extends ConnectRecord<R>> extends App<R> {

        @Override
        protected Schema operatingSchema(R record) {
            return record.valueSchema();
        }

        @Override
        protected Object operatingValue(R record) {
            return record.value();
        }

        @Override
        protected R newRecord(R record, Schema updatedSchema, Object updatedValue) {
            return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), updatedSchema, updatedValue, record.timestamp());
        }

    }

}