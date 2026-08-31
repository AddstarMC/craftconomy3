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
package com.greatmancode.craftconomy3.utils;

import com.greatmancode.craftconomy3.Common;
import com.greatmancode.craftconomy3.storage.StorageEngine;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gives a name back to accounts that had theirs released.
 *
 * When a player joins using a name another account still holds, that name is
 * cleared off the stale account so name lookups stay unambiguous. The stale
 * account is still perfectly valid, it just has no name until we work out what
 * its owner is called now, which is what this task does.
 *
 * It runs off the main thread because resolving a name from a uuid can go to
 * the network, and it is deliberately tolerant: an account whose name cannot
 * be resolved is simply left for the next run.
 */
public class NameBackfillTask implements Runnable {

    @Override
    public void run() {
        if (!Common.isInitialized() || Common.getInstance().getStorageHandler() == null) {
            return;
        }
        StorageEngine engine = Common.getInstance().getStorageHandler().getStorageEngine();
        List<UUID> pending;
        try {
            pending = engine.getNamelessAccountUuids();
        } catch (Exception e) {
            Common.getInstance().getLogger().log(Level.WARNING, "Could not look for accounts missing a name.", e);
            return;
        }
        if (pending.isEmpty()) {
            return;
        }

        int resolved = 0;
        for (UUID uuid : pending) {
            String name;
            try {
                name = Common.getInstance().getServerCaller().getPlayerCaller().getPlayerName(uuid);
            } catch (Exception e) {
                // Name resolution can fail outright; leave this one for next time.
                continue;
            }
            if (name == null || name.isEmpty()) {
                continue;
            }
            try {
                engine.updateUsername(name.toLowerCase(), uuid);
                resolved++;
            } catch (Exception e) {
                Common.getInstance().getLogger().log(Level.WARNING,
                        "Could not restore the name of the account for " + uuid, e);
            }
        }

        if (resolved > 0) {
            Common.getInstance().getLogger().info("Restored the current name of " + resolved
                    + " account(s) whose name had been taken by another player.");
        }
    }
}
