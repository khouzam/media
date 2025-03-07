/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.session;

import static androidx.media3.common.util.Assertions.checkNotNull;

import android.app.PendingIntent;
import android.media.session.MediaSession.Token;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.BundleCompat;
import androidx.media3.common.Player;
import androidx.media3.common.util.BundleCollectionUtil;
import androidx.media3.common.util.Util;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Created by {@link MediaSession} to send its state to the {@link MediaController} when the
 * connection request is accepted.
 */
/* package */ class ConnectionState {

  public final int libraryVersion;

  public final int sessionInterfaceVersion;

  public final IMediaSession sessionBinder;

  @Nullable public final PendingIntent sessionActivity;

  public final SessionCommands sessionCommands;

  public final Player.Commands playerCommandsFromSession;

  public final Player.Commands playerCommandsFromPlayer;

  public final Bundle tokenExtras;

  public final Bundle sessionExtras;

  public final PlayerInfo playerInfo;

  public final ImmutableList<CommandButton> customLayout;

  public final ImmutableList<CommandButton> mediaButtonPreferences;

  @Nullable public final Token platformToken;

  public final ImmutableList<CommandButton> commandButtonsForMediaItems;

  public ConnectionState(
      int libraryVersion,
      int sessionInterfaceVersion,
      IMediaSession sessionBinder,
      @Nullable PendingIntent sessionActivity,
      ImmutableList<CommandButton> customLayout,
      ImmutableList<CommandButton> mediaButtonPreferences,
      ImmutableList<CommandButton> commandButtonsForMediaItems,
      SessionCommands sessionCommands,
      Player.Commands playerCommandsFromSession,
      Player.Commands playerCommandsFromPlayer,
      Bundle tokenExtras,
      Bundle sessionExtras,
      PlayerInfo playerInfo,
      @Nullable Token platformToken) {
    this.libraryVersion = libraryVersion;
    this.sessionInterfaceVersion = sessionInterfaceVersion;
    this.sessionBinder = sessionBinder;
    this.sessionActivity = sessionActivity;
    this.customLayout = customLayout;
    this.mediaButtonPreferences = mediaButtonPreferences;
    this.commandButtonsForMediaItems = commandButtonsForMediaItems;
    this.sessionCommands = sessionCommands;
    this.playerCommandsFromSession = playerCommandsFromSession;
    this.playerCommandsFromPlayer = playerCommandsFromPlayer;
    this.tokenExtras = tokenExtras;
    this.sessionExtras = sessionExtras;
    this.playerInfo = playerInfo;
    this.platformToken = platformToken;
  }

  private static final String FIELD_LIBRARY_VERSION = Util.intToStringMaxRadix(0);
  private static final String FIELD_SESSION_BINDER = Util.intToStringMaxRadix(1);
  private static final String FIELD_SESSION_ACTIVITY = Util.intToStringMaxRadix(2);
  private static final String FIELD_CUSTOM_LAYOUT = Util.intToStringMaxRadix(9);
  private static final String FIELD_MEDIA_BUTTON_PREFERENCES = Util.intToStringMaxRadix(14);
  private static final String FIELD_COMMAND_BUTTONS_FOR_MEDIA_ITEMS = Util.intToStringMaxRadix(13);
  private static final String FIELD_SESSION_COMMANDS = Util.intToStringMaxRadix(3);
  private static final String FIELD_PLAYER_COMMANDS_FROM_SESSION = Util.intToStringMaxRadix(4);
  private static final String FIELD_PLAYER_COMMANDS_FROM_PLAYER = Util.intToStringMaxRadix(5);
  private static final String FIELD_TOKEN_EXTRAS = Util.intToStringMaxRadix(6);
  private static final String FIELD_SESSION_EXTRAS = Util.intToStringMaxRadix(11);
  private static final String FIELD_PLAYER_INFO = Util.intToStringMaxRadix(7);
  private static final String FIELD_SESSION_INTERFACE_VERSION = Util.intToStringMaxRadix(8);
  private static final String FIELD_IN_PROCESS_BINDER = Util.intToStringMaxRadix(10);
  private static final String FIELD_PLATFORM_TOKEN = Util.intToStringMaxRadix(12);

  // Next field key = 15

  public Bundle toBundleForRemoteProcess(int controllerInterfaceVersion) {
    Bundle bundle = new Bundle();
    bundle.putInt(FIELD_LIBRARY_VERSION, libraryVersion);
    BundleCompat.putBinder(bundle, FIELD_SESSION_BINDER, sessionBinder.asBinder());
    bundle.putParcelable(FIELD_SESSION_ACTIVITY, sessionActivity);
    if (!customLayout.isEmpty()) {
      bundle.putParcelableArrayList(
          FIELD_CUSTOM_LAYOUT,
          BundleCollectionUtil.toBundleArrayList(customLayout, CommandButton::toBundle));
    }
    if (!mediaButtonPreferences.isEmpty()) {
      if (controllerInterfaceVersion >= 7) {
        bundle.putParcelableArrayList(
            FIELD_MEDIA_BUTTON_PREFERENCES,
            BundleCollectionUtil.toBundleArrayList(
                mediaButtonPreferences, CommandButton::toBundle));
      } else {
        // Controller doesn't support media button preferences, send the list as a custom layout.
        // TODO: b/332877990 - Improve this logic to take allowed command and session extras for
        //  this controller into account instead of assuming all slots are allowed.
        ImmutableList<CommandButton> customLayout =
            CommandButton.getCustomLayoutFromMediaButtonPreferences(
                mediaButtonPreferences,
                /* backSlotAllowed= */ true,
                /* forwardSlotAllowed= */ true);
        bundle.putParcelableArrayList(
            FIELD_CUSTOM_LAYOUT,
            BundleCollectionUtil.toBundleArrayList(customLayout, CommandButton::toBundle));
      }
    }
    if (!commandButtonsForMediaItems.isEmpty()) {
      bundle.putParcelableArrayList(
          FIELD_COMMAND_BUTTONS_FOR_MEDIA_ITEMS,
          BundleCollectionUtil.toBundleArrayList(
              commandButtonsForMediaItems, CommandButton::toBundle));
    }
    bundle.putBundle(FIELD_SESSION_COMMANDS, sessionCommands.toBundle());
    bundle.putBundle(FIELD_PLAYER_COMMANDS_FROM_SESSION, playerCommandsFromSession.toBundle());
    bundle.putBundle(FIELD_PLAYER_COMMANDS_FROM_PLAYER, playerCommandsFromPlayer.toBundle());
    bundle.putBundle(FIELD_TOKEN_EXTRAS, tokenExtras);
    bundle.putBundle(FIELD_SESSION_EXTRAS, sessionExtras);
    Player.Commands intersectedCommands =
        MediaUtils.intersect(playerCommandsFromSession, playerCommandsFromPlayer);
    bundle.putBundle(
        FIELD_PLAYER_INFO,
        playerInfo
            .filterByAvailableCommands(
                intersectedCommands, /* excludeTimeline= */ false, /* excludeTracks= */ false)
            .toBundleForRemoteProcess(controllerInterfaceVersion));
    bundle.putInt(FIELD_SESSION_INTERFACE_VERSION, sessionInterfaceVersion);
    if (platformToken != null) {
      bundle.putParcelable(FIELD_PLATFORM_TOKEN, platformToken);
    }
    return bundle;
  }

  /**
   * Returns a {@link Bundle} that stores a direct object reference to this class for in-process
   * sharing.
   */
  public Bundle toBundleInProcess() {
    Bundle bundle = new Bundle();
    bundle.putBinder(FIELD_IN_PROCESS_BINDER, new InProcessBinder());
    return bundle;
  }

  /** Restores a {@code ConnectionState} from a {@link Bundle}. */
  public static ConnectionState fromBundle(Bundle bundle) {
    @Nullable IBinder inProcessBinder = bundle.getBinder(FIELD_IN_PROCESS_BINDER);
    if (inProcessBinder instanceof InProcessBinder) {
      return ((InProcessBinder) inProcessBinder).getConnectionState();
    }
    int libraryVersion = bundle.getInt(FIELD_LIBRARY_VERSION, /* defaultValue= */ 0);
    int sessionInterfaceVersion =
        bundle.getInt(FIELD_SESSION_INTERFACE_VERSION, /* defaultValue= */ 0);
    IBinder sessionBinder = checkNotNull(BundleCompat.getBinder(bundle, FIELD_SESSION_BINDER));
    @Nullable PendingIntent sessionActivity = bundle.getParcelable(FIELD_SESSION_ACTIVITY);
    @Nullable
    List<Bundle> customLayoutArrayList = bundle.getParcelableArrayList(FIELD_CUSTOM_LAYOUT);
    ImmutableList<CommandButton> customLayout =
        customLayoutArrayList != null
            ? BundleCollectionUtil.fromBundleList(
                b -> CommandButton.fromBundle(b, sessionInterfaceVersion), customLayoutArrayList)
            : ImmutableList.of();
    @Nullable
    List<Bundle> mediaButtonPreferencesArrayList =
        bundle.getParcelableArrayList(FIELD_MEDIA_BUTTON_PREFERENCES);
    ImmutableList<CommandButton> mediaButtonPreferences =
        mediaButtonPreferencesArrayList != null
            ? BundleCollectionUtil.fromBundleList(
                b -> CommandButton.fromBundle(b, sessionInterfaceVersion),
                mediaButtonPreferencesArrayList)
            : ImmutableList.of();
    @Nullable
    List<Bundle> commandButtonsForMediaItemsArrayList =
        bundle.getParcelableArrayList(FIELD_COMMAND_BUTTONS_FOR_MEDIA_ITEMS);
    ImmutableList<CommandButton> commandButtonsForMediaItems =
        commandButtonsForMediaItemsArrayList != null
            ? BundleCollectionUtil.fromBundleList(
                b -> CommandButton.fromBundle(b, sessionInterfaceVersion),
                commandButtonsForMediaItemsArrayList)
            : ImmutableList.of();
    @Nullable Bundle sessionCommandsBundle = bundle.getBundle(FIELD_SESSION_COMMANDS);
    SessionCommands sessionCommands =
        sessionCommandsBundle == null
            ? SessionCommands.EMPTY
            : SessionCommands.fromBundle(sessionCommandsBundle);
    @Nullable
    Bundle playerCommandsFromPlayerBundle = bundle.getBundle(FIELD_PLAYER_COMMANDS_FROM_PLAYER);
    Player.Commands playerCommandsFromPlayer =
        playerCommandsFromPlayerBundle == null
            ? Player.Commands.EMPTY
            : Player.Commands.fromBundle(playerCommandsFromPlayerBundle);
    @Nullable
    Bundle playerCommandsFromSessionBundle = bundle.getBundle(FIELD_PLAYER_COMMANDS_FROM_SESSION);
    Player.Commands playerCommandsFromSession =
        playerCommandsFromSessionBundle == null
            ? Player.Commands.EMPTY
            : Player.Commands.fromBundle(playerCommandsFromSessionBundle);
    @Nullable Bundle tokenExtras = bundle.getBundle(FIELD_TOKEN_EXTRAS);
    @Nullable Bundle sessionExtras = bundle.getBundle(FIELD_SESSION_EXTRAS);
    @Nullable Bundle playerInfoBundle = bundle.getBundle(FIELD_PLAYER_INFO);
    PlayerInfo playerInfo =
        playerInfoBundle == null
            ? PlayerInfo.DEFAULT
            : PlayerInfo.fromBundle(playerInfoBundle, sessionInterfaceVersion);
    @Nullable Token platformToken = bundle.getParcelable(FIELD_PLATFORM_TOKEN);
    return new ConnectionState(
        libraryVersion,
        sessionInterfaceVersion,
        IMediaSession.Stub.asInterface(sessionBinder),
        sessionActivity,
        customLayout,
        mediaButtonPreferences,
        commandButtonsForMediaItems,
        sessionCommands,
        playerCommandsFromSession,
        playerCommandsFromPlayer,
        tokenExtras == null ? Bundle.EMPTY : tokenExtras,
        sessionExtras == null ? Bundle.EMPTY : sessionExtras,
        playerInfo,
        platformToken);
  }

  private final class InProcessBinder extends Binder {
    public ConnectionState getConnectionState() {
      return ConnectionState.this;
    }
  }
}
