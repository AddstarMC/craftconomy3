/*
 * This file is part of Craftconomy3.
 *
 * Copyright (c) 2011-2013, Greatman <http://github.com/greatman/>
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
package com.greatmancode.craftconomy3.database.tables;

import java.sql.Timestamp;

import com.alta189.simplesave.Field;
import com.alta189.simplesave.Id;
import com.alta189.simplesave.Table;
import com.greatmancode.craftconomy3.Cause;
import com.greatmancode.craftconomy3.LogInfo;

@Table("log")
public class OldLogTable {
	@Id
	public int id;
	@Field
	public int username_id;
	@Field
	public double amount;
	@Field
	public LogInfo type;
	@Field
	public Cause cause;
	@Field
	public Timestamp timestamp;
	@Field
	public String causeReason;
	@Field
	public String currencyName;
	@Field
	public String worldName;
}
