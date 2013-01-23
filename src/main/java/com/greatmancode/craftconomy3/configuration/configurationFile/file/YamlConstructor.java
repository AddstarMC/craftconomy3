/*
 * This file is part of Craftconomy3.
 *
 * Copyright (c) 2011-2012, Greatman <http://github.com/greatman/>
 *
 * Craftconomy3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Craftconomy3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Craftconomy3.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.greatmancode.craftconomy3.configuration.configurationFile.file;

import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

import org.bukkit.configuration.serialization.ConfigurationSerialization;

public class YamlConstructor extends SafeConstructor {
	public YamlConstructor() {
		this.yamlConstructors.put(Tag.MAP, new ConstructCustomObject());
	}

	private class ConstructCustomObject extends ConstructYamlMap {
		@Override
		public Object construct(Node node) {
			if (node.isTwoStepsConstruction()) {
				throw new YAMLException("Unexpected referential mapping structure. Node: " + node);
			}

			Map<?, ?> raw = (Map<?, ?>) super.construct(node);

			if (raw.containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
				Map<String, Object> typed = new LinkedHashMap<String, Object>(raw.size());
				for (Map.Entry<?, ?> entry : raw.entrySet()) {
					typed.put(entry.getKey().toString(), entry.getValue());
				}

				try {
					return ConfigurationSerialization.deserializeObject(typed);
				} catch (IllegalArgumentException ex) {
					throw new YAMLException("Could not deserialize object", ex);
				}
			}

			return raw;
		}

		@Override
		public void construct2ndStep(Node node, Object object) {
			throw new YAMLException("Unexpected referential mapping structure. Node: " + node);
		}
	}
}
