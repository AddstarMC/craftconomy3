/**
 * This file is part of Craftconomy3.
 *
 * Copyright (c) 2011-2016, Greatman <http://github.com/greatman/>
 * Copyright (c) 2017, Aztorius <http://github.com/Aztorius/>
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
package com.greatmancode.craftconomy3.commands.money;

import com.greatmancode.craftconomy3.Cause;
import com.greatmancode.craftconomy3.Common;
import com.greatmancode.craftconomy3.CraftconomyVaultEconomy;
import com.greatmancode.craftconomy3.LogInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Turns a raw log row into a single line of player-facing chat.
 *
 * The stored row is a pair of enums plus a free-text reason whose meaning
 * changes with the cause: for PAYMENT it is the other player, for USER the
 * admin who ran the command, for VAULT the plugin that moved the money. Each
 * combination gets its own phrase so the reader never has to know that.
 */
public final class LogEntryFormatter {

    private LogEntryFormatter() {
    }

    /**
     * Renders one log entry, colours included.
     */
    public static String format(LogCommand.LogEntry entry) {
        // SimpleDateFormat is not thread safe and the log query runs off the
        // main thread, so build them per call rather than sharing instances.
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(entry.timestamp.getTime()));
        String time = new SimpleDateFormat("HH:mm").format(new Date(entry.timestamp.getTime()));
        return Common.getInstance().getLanguageManager()
                .parse("money_log_line", date, time, amount(entry), description(entry));
    }

    /**
     * The signed, coloured amount. Deposits gain, withdrawals lose, and a set
     * is neither, so it gets "=" and a neutral colour.
     *
     * The world is deliberately not passed to the formatter: it would prefix
     * the number with "world: " on a multi group server, which belongs
     * nowhere between the sign and the digits.
     */
    private static String amount(LogCommand.LogEntry entry) {
        String value = Common.getInstance().format(null, entry.currency, entry.amount);
        LogInfo type = parseType(entry.type);
        if (type == LogInfo.DEPOSIT) {
            return Common.getInstance().getLanguageManager().parse("money_log_amount_deposit", value);
        }
        if (type == LogInfo.WITHDRAW) {
            return Common.getInstance().getLanguageManager().parse("money_log_amount_withdraw", value);
        }
        return Common.getInstance().getLanguageManager().parse("money_log_amount_set", value);
    }

    /**
     * The human phrase for a (type, cause) pair. Every phrase has a second
     * form for when the reason is missing, because the reason has always been
     * optional: the three argument deposit/withdraw/set overloads default it
     * to null, and Vault rows written before 3.5.0 never carried one.
     */
    private static String description(LogCommand.LogEntry entry) {
        LogInfo type = parseType(entry.type);
        Cause cause = parseCause(entry.cause);
        String name = usableReason(entry.causeReason);

        // Without a known direction there is no honest way to say "paid" or
        // "received", so such a row is only ever described as an adjustment.
        if (type == null || type == LogInfo.ADMIN_CMD) {
            return phrase("money_log_unknown", name);
        }
        boolean deposit = type == LogInfo.DEPOSIT;

        switch (cause) {
            case PAYMENT:
                return phrase(deposit ? "money_log_payment_received" : "money_log_payment_sent", name);
            case VAULT:
            case PLUGIN:
            case SPOUT:
                return phrase(deposit ? "money_log_plugin_deposit" : "money_log_plugin_withdraw", name);
            case USER:
                if (type == LogInfo.SET) {
                    return phrase("money_log_admin_set", name);
                }
                return phrase(deposit ? "money_log_admin_deposit" : "money_log_admin_withdraw", name);
            case BANK_DEPOSIT:
                return phrase("money_log_bank_deposit", name);
            case BANK_WITHDRAW:
                return phrase("money_log_bank_withdraw", name);
            case BANK_CREATION:
                return phrase("money_log_bank_creation", null);
            case BANK_DELETE:
                return phrase("money_log_bank_delete", name);
            case EXCHANGE:
                return phrase("money_log_exchange", name);
            case PAYDAY_WAGE:
                return phrase("money_log_payday_wage", null);
            case PAYDAY_TAX:
                return phrase("money_log_payday_tax", null);
            case CONVERT:
                return phrase("money_log_convert", null);
            default:
                return phrase("money_log_unknown", name);
        }
    }

    /**
     * Picks the named or the anonymous form of a phrase. The anonymous key is
     * the named one with "_unknown" appended.
     */
    private static String phrase(String key, String name) {
        if (name == null) {
            return Common.getInstance().getLanguageManager().getString(key + "_unknown");
        }
        return Common.getInstance().getLanguageManager().parse(key, name);
    }

    /**
     * A reason is only worth printing when it names something other than us.
     *
     * Vault rows carry the plugin's own name rather than the plugin that
     * actually moved the money, which would read as "Withdrawn by
     * Craftconomy3" -- worse than saying nothing, since it blames this plugin
     * for a purchase made in someone else's shop. Rows written before 3.5.0
     * have no reason at all. Both collapse to the anonymous phrase.
     */
    private static String usableReason(String causeReason) {
        if (causeReason == null || causeReason.trim().isEmpty()) {
            return null;
        }
        if (causeReason.equalsIgnoreCase(CraftconomyVaultEconomy.NAME)) {
            return null;
        }
        return causeReason;
    }

    /**
     * The type and cause columns are free text, so a row written by an older
     * version or hand edited in the database can hold anything. Returns null
     * when the value is not a type we know, which every caller reads as
     * "neither a gain nor a loss".
     */
    private static LogInfo parseType(String type) {
        if (type != null) {
            try {
                return LogInfo.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Falls through to the neutral treatment below.
            }
        }
        return null;
    }

    private static Cause parseCause(String cause) {
        if (cause != null) {
            try {
                return Cause.valueOf(cause.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Falls through to UNKNOWN below.
            }
        }
        return Cause.UNKNOWN;
    }
}
