/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.variables;

import org.eclipse.jdt.annotation.Nullable;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ch.njol.skript.config.SectionNode;

public class H2Storage extends SQLStorage {

	public H2Storage(String name) {
		//super(name, "CREATE TABLE IF NOT EXISTS %s (`id` CHAR(36) PRIMARY KEY, `data` TEXT);");
		//CREATE TABLE IF NOT EXISTS variables21 (name VARCHAR(380) NOT NULL PRIMARY KEY, type VARCHAR(50), value BLOB(10000), update_guid CHAR(36) NOT NULL)
		super(name, "CREATE TABLE IF NOT EXISTS %s (" +
				"`name`         VARCHAR2(" + MAX_VARIABLE_NAME_LENGTH + ")  NOT NULL  PRIMARY KEY," +
				"`type`         VARCHAR2(" + MAX_CLASS_CODENAME_LENGTH + ")," +
				"`value`        TEXT(" + MAX_VALUE_SIZE + ")," +
				"`update_guid`  CHAR(36)  NOT NULL" +
				");");
	}

	@Override
	@Nullable
	public HikariDataSource initialize(SectionNode config) {
		if (file == null)
			return null;
		HikariConfig configuration = new HikariConfig();
		configuration.setPoolName("H2-Pool");
		configuration.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
		configuration.setConnectionTestQuery("VALUES 1");

		String url = "";
		if (config.get("memory", "false").equalsIgnoreCase("true"))
			url += "mem:";
		url += "file:" + file.getAbsolutePath();
		configuration.addDataSourceProperty("URL", "jdbc:h2:" + url);
		configuration.addDataSourceProperty("user", config.get("user", ""));
		configuration.addDataSourceProperty("password", config.get("password", ""));
		configuration.addDataSourceProperty("description", config.get("description", ""));
		return new HikariDataSource(configuration);
	}

	@Override
	protected boolean requiresFile() {
		return true;
	}

}
