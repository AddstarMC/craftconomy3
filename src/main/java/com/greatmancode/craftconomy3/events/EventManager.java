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
package com.greatmancode.craftconomy3.events;

import com.greatmancode.craftconomy3.Common;
import com.greatmancode.craftconomy3.account.Account;
import com.greatmancode.tools.events.interfaces.EventHandler;
import com.greatmancode.tools.events.interfaces.Listener;
import com.greatmancode.tools.events.playerEvent.PlayerJoinEvent;
import com.greatmancode.tools.events.playerEvent.PlayerQuitEvent;
import com.greatmancode.tools.events.playerEvent.PreJoinEvent;

import java.util.logging.Level;

/**
 * This class contains code shared for events.
 */
public class EventManager implements Listener {

    /**
     * Event handler for when a player is connecting to the server.
     *
     * @param event The PlayerJoinEvent associated with the event
     */
    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent event) {
        if (!Common.getInstance().getMainConfig().getBoolean("System.Setup",true)) {
            if (Common.getInstance().getMainConfig().getBoolean("System.CreateOnLogin",false)) {
                Account acc = Common.getInstance().getAccountManager().getAccount(event.getP().getName(), false);
                if(acc != null)Common.getInstance().getLogger().log(Level.FINER,"Account retrieved for "+ event.getP().getDisplayName());
                else
                    Common.getInstance().getLogger().log(Level.FINER,"Account retrieval failed for "+ event.getP().getDisplayName());

            }
        }
    }

    /**
     * Clear a name off any account that is not this player's.
     *
     * Only called when the name a player is joining under is not already
     * recorded against their uuid, so an unchanged login does no extra work.
     */
    private void releaseNameFromStaleAccounts(String name, java.util.UUID uuid) {
        int released = Common.getInstance().getStorageHandler().getStorageEngine()
                .releaseNameFromOtherAccounts(name, uuid);
        if (released > 0) {
            Common.getInstance().getAccountManager().clearCache(name);
            Common.getInstance().getLogger().info("Released the name '" + name + "' from "
                    + released + " stale account(s) so " + uuid + " can use it.");
        }
    }

    /**
     * Drop a player's cached account once they have left.
     *
     * The cache exists to save a lookup per Vault call while somebody is
     * playing; keeping it after they leave only grows the map and lets flags
     * such as infiniteMoney go stale against the other servers sharing this
     * database.
     *
     * The removal is delayed so anything still settling a transaction as the
     * player disconnects keeps its cache hit, and it is skipped if they have
     * come back in the meantime, which happens routinely when moving between
     * servers behind the proxy.
     */
    @EventHandler
    public void PlayerQuitEvent(final PlayerQuitEvent event) {
        if (Common.getInstance().getMainConfig().getBoolean("System.Setup", true)) {
            return;
        }
        final String name = event.getName() != null ? event.getName().toLowerCase() : null;
        if (name == null) {
            return;
        }
        // The scheduler multiplies its delay by 20 ticks, so this is 30 seconds.
        Common.getInstance().getServerCaller().getSchedulerCaller().delay(new Runnable() {
            @Override
            public void run() {
                if (event.getUuid() != null
                        && Common.getInstance().getServerCaller().getPlayerCaller().isOnline(event.getUuid())) {
                    // They reconnected; their cached account is in use again.
                    return;
                }
                Common.getInstance().getAccountManager().clearCache(name);
            }
        }, 30, false);
    }

    @EventHandler
    public void PreJoinEvent(PreJoinEvent event) {
        if (!Common.getInstance().getMainConfig().getBoolean("System.Setup",true)) {
            String name = event.getName().toLowerCase();

            //We search if the UUID is in the database
            Account account = Common.getInstance().getStorageHandler().getStorageEngine().getAccount(event.getUuid());
            if (account != null && !name.equals(account.getAccountName())) {
                // This player is using a name we have not seen them under. If
                // somebody who renamed away still holds it, take it off their
                // account first, otherwise both rows end up with the same name
                // and every name based lookup for either of them fails. That
                // account keeps its balance and uuid; only the name goes, and
                // the backfill task restores their current one later.
                releaseNameFromStaleAccounts(name, event.getUuid());
                Common.getInstance().getAccountManager().clearCache(account.getAccountName());
                Common.getInstance().getStorageHandler().getStorageEngine().updateUsername(name, event.getUuid());
            } else if (account == null){
                // First time we have seen this uuid, so the name may still be
                // held by whoever used it before.
                releaseNameFromStaleAccounts(name, event.getUuid());
                //We set deh UUID
                Common.getInstance().getStorageHandler().getStorageEngine().updateUUID(name, event.getUuid());
            }
        }
    }
}
