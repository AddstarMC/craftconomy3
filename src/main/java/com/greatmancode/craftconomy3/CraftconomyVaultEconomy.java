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
package com.greatmancode.craftconomy3;

import com.greatmancode.craftconomy3.account.Account;
import com.greatmancode.craftconomy3.currency.Currency;
import com.greatmancode.craftconomy3.groups.WorldGroupsManager;
import com.greatmancode.tools.entities.Player;
import com.greatmancode.tools.utils.VaultEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

import java.util.ArrayList;
import java.util.List;

/**
 * Exposes Craftconomy to Vault.
 *
 * Historically Craftconomy did not register itself with Vault at all; Vault
 * carried a bundled Economy_Craftconomy3 hook instead. That hook was removed
 * from Vault in 2022, so registering ourselves is what keeps the economy
 * working on Vault builds newer than 1.7.3.
 *
 * Vault's API is single-currency and has no concept of world groups, so every
 * call here resolves against the default currency. Balance lookups use the
 * world group Vault supplies, falling back to the default group.
 */
public class CraftconomyVaultEconomy extends VaultEconomy {

    private static final String NAME = "Craftconomy3";

    @Override
    public boolean isEnabled() {
        return Common.isInitialized();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean hasBankSupport() {
        return true;
    }

    @Override
    public int fractionalDigits() {
        // Craftconomy truncates to two decimal places, see Account#format.
        return 2;
    }

    @Override
    public String format(double amount) {
        return Common.getInstance().format(null, defaultCurrency(), amount);
    }

    @Override
    public String currencyNameSingular() {
        Currency currency = defaultCurrency();
        return currency != null ? currency.getName() : "";
    }

    @Override
    public String currencyNamePlural() {
        Currency currency = defaultCurrency();
        return currency != null ? currency.getPlural() : "";
    }

    // -- Player accounts ---------------------------------------------------

    @Override
    public boolean hasAccount(Player player) {
        return player != null
                && player.getName() != null
                && Common.getInstance().getAccountManager().exist(player.getName(), false);
    }

    @Override
    public boolean createPlayerAccount(Player player) {
        if (player == null || player.getName() == null) {
            return false;
        }
        if (Common.getInstance().getAccountManager().exist(player.getName(), false)) {
            return false;
        }
        return Common.getInstance().getAccountManager().getAccount(player.getName(), false) != null;
    }

    @Override
    public double getBalance(Player player, String world) {
        Account account = playerAccount(player);
        if (account == null) {
            return 0;
        }
        return account.getBalance(worldGroup(world), currencyName());
    }

    @Override
    public boolean has(Player player, String world, double amount) {
        Account account = playerAccount(player);
        return account != null && account.hasEnough(amount, worldGroup(world), currencyName());
    }

    @Override
    public EconomyResponse withdrawPlayer(Player player, String world, double amount) {
        if (amount < 0) {
            return failure("Cannot withdraw negative funds");
        }
        Account account = playerAccount(player);
        if (account == null) {
            return failure("That account does not exist!");
        }
        String group = worldGroup(world);
        if (!account.hasEnough(amount, group, currencyName())) {
            return new EconomyResponse(0, account.getBalance(group, currencyName()),
                    ResponseType.FAILURE, "Insufficient funds");
        }
        double balance = account.withdraw(amount, group, currencyName(), Cause.VAULT, NAME);
        return new EconomyResponse(amount, balance, ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse depositPlayer(Player player, String world, double amount) {
        if (amount < 0) {
            return failure("Cannot deposit negative funds");
        }
        Account account = playerAccount(player);
        if (account == null) {
            return failure("That account does not exist!");
        }
        double balance = account.deposit(amount, worldGroup(world), currencyName(), Cause.VAULT, NAME);
        return new EconomyResponse(amount, balance, ResponseType.SUCCESS, "");
    }

    // -- Bank accounts -----------------------------------------------------

    @Override
    public EconomyResponse createBank(String name, Player player) {
        if (player == null || player.getName() == null) {
            return failure("Unable to create that bank account. No owner was given!");
        }
        if (Common.getInstance().getAccountManager().exist(name, true)) {
            return failure("Unable to create that bank account. It already exists!");
        }
        Account account = Common.getInstance().getAccountManager().getAccount(name, true);
        if (account == null) {
            return failure("Unable to create that bank account.");
        }
        account.getAccountACL().set(player.getName(), true, true, true, true, true);
        return success(0);
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        if (Common.getInstance().getAccountManager().delete(name, true)) {
            return success(0);
        }
        return failure("Unable to delete that bank account.");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        Account account = bankAccount(name);
        if (account == null) {
            return failure("That bank does not exist!");
        }
        return success(account.getBalance(WorldGroupsManager.DEFAULT_GROUP_NAME, bankCurrencyName()));
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        Account account = bankAccount(name);
        if (account == null) {
            return failure("That bank does not exist!");
        }
        double balance = account.getBalance(WorldGroupsManager.DEFAULT_GROUP_NAME, bankCurrencyName());
        if (account.hasEnough(amount, WorldGroupsManager.DEFAULT_GROUP_NAME, bankCurrencyName())) {
            return new EconomyResponse(0, balance, ResponseType.SUCCESS, "");
        }
        return new EconomyResponse(0, balance, ResponseType.FAILURE, "The bank does not have enough money!");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        if (amount < 0) {
            return failure("Cannot withdraw negative funds");
        }
        Account account = bankAccount(name);
        if (account == null) {
            return failure("That bank does not exist!");
        }
        if (!account.hasEnough(amount, WorldGroupsManager.DEFAULT_GROUP_NAME, bankCurrencyName())) {
            return new EconomyResponse(0,
                    account.getBalance(WorldGroupsManager.DEFAULT_GROUP_NAME, bankCurrencyName()),
                    ResponseType.FAILURE, "The bank does not have enough money!");
        }
        double balance = account.withdraw(amount, WorldGroupsManager.DEFAULT_GROUP_NAME,
                bankCurrencyName(), Cause.BANK_WITHDRAW, NAME);
        return new EconomyResponse(amount, balance, ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        if (amount < 0) {
            return failure("Cannot deposit negative funds");
        }
        Account account = bankAccount(name);
        if (account == null) {
            return failure("That bank does not exist!");
        }
        double balance = account.deposit(amount, WorldGroupsManager.DEFAULT_GROUP_NAME,
                bankCurrencyName(), Cause.BANK_DEPOSIT, NAME);
        return new EconomyResponse(amount, balance, ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse isBankOwner(String name, Player player) {
        if (player == null || player.getName() == null) {
            return failure("That player does not exist!");
        }
        Account account = bankAccount(name);
        if (account == null) {
            return failure("That bank does not exist!");
        }
        if (account.getAccountACL().isOwner(player.getName())) {
            return success(0);
        }
        return failure("That player is not the owner of that bank account!");
    }

    @Override
    public EconomyResponse isBankMember(String name, Player player) {
        if (player == null || player.getName() == null) {
            return failure("That player does not exist!");
        }
        Account account = bankAccount(name);
        if (account == null) {
            return failure("That bank does not exist!");
        }
        // A member is anybody allowed to move money in or out of the account.
        if (account.getAccountACL().canDeposit(player.getName())
                && account.getAccountACL().canWithdraw(player.getName())) {
            return success(0);
        }
        return failure("That player is not a member of that bank account!");
    }

    @Override
    public List<String> getBanks() {
        List<String> banks = Common.getInstance().getAccountManager().getAllAccounts(true);
        return banks != null ? banks : new ArrayList<String>();
    }

    // -- Helpers -----------------------------------------------------------

    private Currency defaultCurrency() {
        return Common.getInstance().getCurrencyManager().getDefaultCurrency();
    }

    private String currencyName() {
        Currency currency = defaultCurrency();
        return currency != null ? currency.getName() : null;
    }

    private String bankCurrencyName() {
        Currency currency = Common.getInstance().getCurrencyManager().getDefaultBankCurrency();
        return currency != null ? currency.getName() : currencyName();
    }

    /**
     * Vault has no notion of world groups and passes a plain world name, or
     * null when it does not care. Resolve that to a Craftconomy world group.
     */
    private String worldGroup(String world) {
        if (world == null) {
            return WorldGroupsManager.DEFAULT_GROUP_NAME;
        }
        return Common.getInstance().getWorldGroupManager().getWorldGroupName(world);
    }

    private Account playerAccount(Player player) {
        if (player == null || player.getName() == null) {
            return null;
        }
        return Common.getInstance().getAccountManager().getAccount(player.getName(), false);
    }

    private Account bankAccount(String name) {
        if (name == null || !Common.getInstance().getAccountManager().exist(name, true)) {
            return null;
        }
        return Common.getInstance().getAccountManager().getAccount(name, true);
    }

    private EconomyResponse success(double balance) {
        return new EconomyResponse(0, balance, ResponseType.SUCCESS, "");
    }

    private EconomyResponse failure(String message) {
        return new EconomyResponse(0, 0, ResponseType.FAILURE, message);
    }
}
