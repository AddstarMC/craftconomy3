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
package com.greatmancode.craftconomy3.account;

import com.greatmancode.craftconomy3.Cause;
import com.greatmancode.craftconomy3.Common;
import com.greatmancode.craftconomy3.LogInfo;
import com.greatmancode.craftconomy3.currency.Currency;
import com.greatmancode.tools.events.event.EconomyChangeEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Represents a economy account.
 *
 * @author greatman
 */
public class Account {
    private int id;
    private AccountACL acl;
    private boolean bankAccount, infiniteMoney, ignoreACL;
    private String name;

    /**
     * Load a account. Creates one if it doesn't exist.
     *
     * @param id The unique id used in the database as primary key
     * @param name The account name
     * @param bankAccount If the account is a bank account
     * @param infiniteMoney If the account have no money limit
     * @param ignoreACL If the account ignore it's ACL values (Bank only)
     */
    public Account(int id, String name, boolean bankAccount, boolean infiniteMoney, boolean ignoreACL) {
        this.id = id;
        this.name = name;
        this.bankAccount = bankAccount;
        this.infiniteMoney = infiniteMoney;
        this.ignoreACL = ignoreACL;
        if (isBankAccount()) {
            acl = new AccountACL(this);
        }
    }

    /**
     * Returns the account name.
     *
     * @return The account name
     */
    public String getAccountName() {
        return name;
    }

    /**
     * Checks if this account is a bank account
     *
     * @return True if this account is a bank account, else false
     */
    public boolean isBankAccount() {
        return bankAccount;
    }

    /**
     * Get the account ACL. Only used with a bank account
     *
     * @return The account ACL if it's a bank account, else null
     */
    public AccountACL getAccountACL() {
        AccountACL accountAcl = null;
        if (isBankAccount()) {
            accountAcl = acl;
        }
        return accountAcl;
    }

    /**
     * Get the whole account balance
     *
     * @return A list of all account balance
     */
    public List<Balance> getAllBalance() {
        return Common.getInstance().getStorageHandler().getStorageEngine().getAllBalance(this);
    }

    /**
     * Get the whole account balance in a certain world / world group
     *
     * @param world The world / world group to search in
     * @return A list of Balance
     */
    public List<Balance> getAllWorldBalance(String world) {
        if (!Common.getInstance().getWorldGroupManager().worldGroupExist(world)) {
            world = Common.getInstance().getWorldGroupManager().getWorldGroupName(world);
        }
        return Common.getInstance().getStorageHandler().getStorageEngine().getAllWorldBalance(this, world);
    }

    /**
     * Get's the player balance. Sends double.MIN_NORMAL in case of a error
     *
     * @param world        The world / world group to search in
     * @param currencyName The currency Name
     * @return The balance. If the account has infinite money. Double.MAX_VALUE is returned.
     */
    public double getBalance(String world, String currencyName) {
        double balance = Double.MIN_NORMAL;
        if (!Common.getInstance().getWorldGroupManager().worldGroupExist(world)) {
            world = Common.getInstance().getWorldGroupManager().getWorldGroupName(world);
        }
        Currency currency = Common.getInstance().getCurrencyManager().getCurrency(currencyName);
        if (currency != null) {
            if (!hasInfiniteMoney()) {
                balance = Common.getInstance().getStorageHandler().getStorageEngine().getBalance(this, currency, world);
            } else {
                balance = Double.MAX_VALUE;
            }
        }
        return format(balance);
    }

    /**
     * Adds a certain amount of money in the account
     *
     * @param amount       The amount of money to add
     * @param world        The World / World group we want to add money in
     * @param currencyName The currency we want to add money in
     * @return The new balance. If the account has infinite money. Double.MAX_VALUE is returned.
     * @deprecated use {@link #deposit(double, String, String, com.greatmancode.craftconomy3.Cause, String)}
     */
    @Deprecated
    public double deposit(double amount, String world, String currencyName) {
        return deposit(amount, world, currencyName, Cause.UNKNOWN, null);
    }

    /**
     * Adds a certain amount of money in the account
     *
     * @param amount       The amount of money to add
     * @param world        The World / World group we want to add money in
     * @param currencyName The currency we want to add money in
     * @param cause        The cause of the change.
     * @param causeReason  The reason of the cause
     * @return The new balance. If the account has infinite money. Double.MAX_VALUE is returned.
     */
    public double deposit(double amount, String world, String currencyName, Cause cause, String causeReason) {
        if (!Common.getInstance().getWorldGroupManager().worldGroupExist(world)) {
            world = Common.getInstance().getWorldGroupManager().getWorldGroupName(world);
        }
        Currency currency = Common.getInstance().getCurrencyManager().getCurrency(currencyName);
        double result = 0;
        if (currency != null) {
            if (!hasInfiniteMoney()) {
                Double newBalance = Common.getInstance().getStorageHandler().getStorageEngine()
                        .changeBalance(this, format(amount), currency, world, null);
                if (newBalance == null) {
                    return format(getBalance(world, currencyName));
                }
                result = newBalance;
                Common.getInstance().writeLog(LogInfo.DEPOSIT, cause, causeReason, this, amount, currency, world);
                Common.getInstance().getServerCaller().throwEvent(new EconomyChangeEvent(this.getAccountName(), result));
            } else {
                result = Double.MAX_VALUE;
            }
        }

        return format(result);
    }

    /**
     * withdraw a certain amount of money in the account
     *
     * @param amount       The amount of money to withdraw
     * @param world        The World / World group we want to withdraw money from
     * @param currencyName The currency we want to withdraw money from
     * @return The new balance. If the account has infinite money. Double.MAX_VALUE is returned.
     * @deprecated use {@link #withdraw(double, String, String, com.greatmancode.craftconomy3.Cause, String)}
     */
    @Deprecated
    public double withdraw(double amount, String world, String currencyName) {
        return withdraw(amount, world, currencyName, Cause.UNKNOWN, null);
    }

    /**
     * withdraw a certain amount of money in the account
     *
     * @param amount       The amount of money to withdraw
     * @param world        The World / World group we want to withdraw money from
     * @param currencyName The currency we want to withdraw money from
     * @param cause        The cause of the change.
     * @param causeReason  The reason of the cause.
     * @return The new balance. If the account has infinite money. Double.MAX_VALUE is returned.
     */
    public double withdraw(double amount, String world, String currencyName, Cause cause, String causeReason) {
        if (!Common.getInstance().getWorldGroupManager().worldGroupExist(world)) {
            world = Common.getInstance().getWorldGroupManager().getWorldGroupName(world);
        }
        Currency currency = Common.getInstance().getCurrencyManager().getCurrency(currencyName);
        double result = 0;
        if (currency != null) {
            if (!hasInfiniteMoney()) {
                // The floor is applied inside the same statement as the change,
                // so a concurrent spend cannot slip between a check and a write.
                Double newBalance = Common.getInstance().getStorageHandler().getStorageEngine()
                        .changeBalance(this, -format(amount), currency, world, 0.0);
                if (newBalance == null) {
                    // Not enough money any more; nothing was written.
                    return format(getBalance(world, currencyName));
                }
                result = newBalance;
                Common.getInstance().writeLog(LogInfo.WITHDRAW, cause, causeReason, this, amount, currency, world);
                Common.getInstance().getServerCaller().throwEvent(new EconomyChangeEvent(this.getAccountName(), result));
            } else {
                result = Double.MAX_VALUE;
            }
        }
        return format(result);
    }

    /**
     * set a certain amount of money in the account
     *
     * @param amount       The amount of money to set
     * @param world        The World / World group we want to set money to
     * @param currencyName The currency we want to set money to
     * @return The new balance
     * @deprecated use {@link #set(double, String, String, com.greatmancode.craftconomy3.Cause, String)}
     */
    @Deprecated
    public double set(double amount, String world, String currencyName) {
        return set(amount, world, currencyName, Cause.UNKNOWN, null);
    }

    /**
     * set a certain amount of money in the account
     *
     * @param amount       The amount of money to set
     * @param world        The World group we want to set money to
     * @param currencyName The currency we want to set money to
     * @param cause        The cause of the change.
     * @param causeReason  The reason of the cause.
     * @return The new balance. If the account has infinite money. Double.MAX_VALUE is returned.
     */
    public double set(double amount, String world, String currencyName, Cause cause, String causeReason) {
        double result = 0;
        if (!Common.getInstance().getWorldGroupManager().worldGroupExist(world)) {
            world = Common.getInstance().getWorldGroupManager().getWorldGroupName(world);
        }
        amount = format(amount);
        if (!Common.getInstance().getWorldGroupManager().worldGroupExist(world)) {
            world = Common.getInstance().getWorldGroupManager().getWorldGroupName(world);
        }
        Currency currency = Common.getInstance().getCurrencyManager().getCurrency(currencyName);
        if (currency != null) {
            if (!hasInfiniteMoney()) {
                result = Common.getInstance().getStorageHandler().getStorageEngine().setBalance(this, amount, currency, world);
                Common.getInstance().writeLog(LogInfo.SET, cause, causeReason, this, amount, currency, world);
                Common.getInstance().getServerCaller().throwEvent(new EconomyChangeEvent(this.getAccountName(), result));
            }
        }
        return format(result);
    }

    /**
     * Move money from this account to another as a single unit of work.
     *
     * Unlike calling withdraw and then deposit, both legs commit together, so
     * a failure part way through cannot leave money debited from the payer but
     * never credited to the payee.
     *
     * @param destination  The account to credit
     * @param amount       The amount to move
     * @param world        The World / World group
     * @param currencyName The currency to move
     * @param cause        The cause of the change
     * @param causeReason  The reason of the cause
     * @return true if the money moved, false if there were insufficient funds
     */
    public boolean transfer(Account destination, double amount, String world, String currencyName, Cause cause, String causeReason) {
        if (!Common.getInstance().getWorldGroupManager().worldGroupExist(world)) {
            world = Common.getInstance().getWorldGroupManager().getWorldGroupName(world);
        }
        Currency currency = Common.getInstance().getCurrencyManager().getCurrency(currencyName);
        if (currency == null) {
            return false;
        }
        double value = format(amount);

        // An infinite-money account is not backed by a real balance row, so
        // there is only ever one real side to write and nothing to keep
        // consistent between the two.
        if (hasInfiniteMoney() || destination.hasInfiniteMoney()) {
            if (!hasInfiniteMoney()) {
                Double remaining = Common.getInstance().getStorageHandler().getStorageEngine()
                        .changeBalance(this, -value, currency, world, 0.0);
                if (remaining == null) {
                    return false;
                }
                Common.getInstance().writeLog(LogInfo.WITHDRAW, cause, causeReason, this, value, currency, world);
                Common.getInstance().getServerCaller().throwEvent(
                        new EconomyChangeEvent(this.getAccountName(), remaining));
            }
            if (!destination.hasInfiniteMoney()) {
                destination.deposit(value, world, currencyName, cause, causeReason);
            }
            return true;
        }

        boolean moved = Common.getInstance().getStorageHandler().getStorageEngine()
                .transfer(this, destination, value, currency, world);
        if (!moved) {
            return false;
        }

        Common.getInstance().writeLog(LogInfo.WITHDRAW, cause, causeReason, this, value, currency, world);
        Common.getInstance().writeLog(LogInfo.DEPOSIT, cause, causeReason, destination, value, currency, world);
        Common.getInstance().getServerCaller().throwEvent(
                new EconomyChangeEvent(this.getAccountName(), getBalance(world, currencyName)));
        Common.getInstance().getServerCaller().throwEvent(
                new EconomyChangeEvent(destination.getAccountName(), destination.getBalance(world, currencyName)));
        return true;
    }

    /**
     * Convert money from one currency to another within this account, as a
     * single unit of work. Both sides commit together, so a failure cannot
     * take the source currency without granting the destination one.
     *
     * @param fromAmount   The amount to convert
     * @param fromCurrency The currency being spent
     * @param toAmount     The amount to receive after the exchange rate
     * @param toCurrency   The currency being received
     * @param world        The World / World group
     * @param cause        The cause of the change
     * @param causeReason  The reason of the cause
     * @return true if the exchange happened, false if there were insufficient funds
     */
    public boolean exchange(double fromAmount, Currency fromCurrency, double toAmount, Currency toCurrency,
                            String world, Cause cause, String causeReason) {
        if (!Common.getInstance().getWorldGroupManager().worldGroupExist(world)) {
            world = Common.getInstance().getWorldGroupManager().getWorldGroupName(world);
        }
        if (fromCurrency == null || toCurrency == null) {
            return false;
        }
        if (hasInfiniteMoney()) {
            return true;
        }
        double take = format(fromAmount);
        double give = format(toAmount);
        boolean done = Common.getInstance().getStorageHandler().getStorageEngine()
                .transfer(this, this, take, fromCurrency, give, toCurrency, world);
        if (!done) {
            return false;
        }
        Common.getInstance().writeLog(LogInfo.WITHDRAW, cause, causeReason, this, take, fromCurrency, world);
        Common.getInstance().writeLog(LogInfo.DEPOSIT, cause, causeReason, this, give, toCurrency, world);
        Common.getInstance().getServerCaller().throwEvent(
                new EconomyChangeEvent(this.getAccountName(), getBalance(world, toCurrency.getName())));
        return true;
    }

    /**
     * Checks if we have enough money in a certain balance
     *
     * @param amount       The amount of money to check
     * @param worldName    The World / World group we want to check
     * @param currencyName The currency we want to check
     * @return True if there's enough money. Else false
     */
    public boolean hasEnough(double amount, String worldName, String currencyName) {
        boolean result = false;
        amount = format(amount);
        if (!Common.getInstance().getWorldGroupManager().worldGroupExist(worldName)) {
            worldName = Common.getInstance().getWorldGroupManager().getWorldGroupName(worldName);
        }
        Currency currency = Common.getInstance().getCurrencyManager().getCurrency(currencyName);
        if (currency != null && (getBalance(worldName, currencyName) >= amount || hasInfiniteMoney())) {
            result = true;
        }
        return result;
    }

    /**
     * Returns the world that the player is currently in
     *
     * @param  playerUUID The player uuid.
     * @return The world name that the player is currently in or any if he is not online/Multiworld system not enabled
     */
    private static String getWorldPlayerCurrentlyIn(UUID playerUUID) {
        return Common.getInstance().getServerCaller().getPlayerCaller().getPlayerWorld(playerUUID);
    }

    /**
     * Retrieve the world group of the player
     *
     * @param  playerUUID The player uuid
     * @return The worldGroup of the player.
     */
    public static String getWorldGroupOfPlayerCurrentlyIn(UUID playerUUID) {
        return Common.getInstance().getWorldGroupManager().getWorldGroupName(getWorldPlayerCurrentlyIn(playerUUID));
    }

    /**
     * Sets the account to have infinite money.
     *
     * @param infinite True if the account should have infinite money. Else false.
     */
    public void setInfiniteMoney(boolean infinite) {
        Common.getInstance().getStorageHandler().getStorageEngine().setInfiniteMoney(this, infinite);
        infiniteMoney = infinite;
    }

    /**
     * Checks if the account have infinite money
     *
     * @return True if the account have infinite money. Else false.
     */
    public boolean hasInfiniteMoney() {
        return infiniteMoney;
    }

    /**
     * Format the value to be something less problematic.
     * Example: 50.00999999995 will become 50.00
     *
     * @param value The double to format
     * @return The formatted double
     */
    public static double format(double value) {
        if (value == Double.MAX_VALUE || value == Double.MIN_NORMAL) {
            return value;
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return value;
        }

        // Truncate to two decimal places, matching long-standing behaviour:
        // a sub-cent amount is never rounded up into a whole cent.
        //
        // This uses DOWN rather than the previous Math.floor, which rounded
        // toward negative infinity and so treated negative values differently
        // from positive ones. Going through BigDecimal also avoids the
        // multiply-by-100 float artefacts of the old implementation.
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.DOWN)
                .doubleValue();
    }

    /**
     * Check if the ACL is ignored for a bank account. That means that there will be no protection on the account and anybody can deposit/withdraw from it!
     *
     * @return True if the ACL is ignored else false.
     */
    public boolean ignoreACL() {
        return ignoreACL;
    }

    /**
     * Sets if a account should ignore his ACL. Only works on Bank accounts.
     *
     * @param ignore If the ACL is ignored or not
     */
    public void setIgnoreACL(boolean ignore) {
        Common.getInstance().getStorageHandler().getStorageEngine().setIgnoreACL(this, ignore);
        ignoreACL = ignore;
    }

    /**
     * Returns the unique id used as primary key in the database
     *
     * @return the unique id
     */
    public int getId() {
        return this.id;
    }
}
