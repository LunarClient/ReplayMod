package com.replaymod.core.utils;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.net.PercentEscaper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.AbstractGuiScrollable;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScrollable;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.GuiInfoPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.versions.Image;
import de.johni0702.minecraft.gui.versions.MCVer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=11400
//#else
import org.lwjgl.input.Keyboard;
//#endif

//#if MC>=10800
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
//#else
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.entity.AbstractClientPlayer;
//$$ import net.minecraft.entity.player.EntityPlayer;
//#endif

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import static com.replaymod.core.versions.MCVer.getMinecraft;

public class Utils {
    private static Logger LOGGER = LogManager.getLogger();

    private static InputStream getResourceAsStream(String path) {
        return Utils.class.getResourceAsStream(path);
    }

    public static final Image DEFAULT_THUMBNAIL;

    static {
        Image thumbnail;
        try {
            thumbnail = Image.read(getResourceAsStream("/default_thumb.jpg"));
        } catch (Exception e) {
            thumbnail = new Image(1, 1);
            e.printStackTrace();
        }
        DEFAULT_THUMBNAIL = thumbnail;
    }


    /**
     * Neither the root certificate of LetsEncrypt nor the root that cross-signed it is included in the default
     * Java keystore prior to 8u101.
     * Therefore whenever a connection to the replaymod.com site is made, this SSLContext has to be used instead.
     * It has been constructed to include the necessary root certificates.
     * @see #SSL_SOCKET_FACTORY
     */
    public static final SSLContext SSL_CONTEXT;

    /**
     * @see #SSL_CONTEXT
     */
    public static final SSLSocketFactory SSL_SOCKET_FACTORY;

    static {
        // Largely from https://community.letsencrypt.org/t/134/37
        try (InputStream in = getResourceAsStream("/dst_root_ca_x3.pem")){
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(in);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("1", certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustManagerFactory.getTrustManagers(), null);
            SSL_CONTEXT = ctx;
            SSL_SOCKET_FACTORY = ctx.getSocketFactory();
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public static String convertSecondsToShortString(int seconds) {
        int hours = seconds/(60*60);
        int min = seconds/60 - hours*60;
        int sec = seconds - ((min*60) + (hours*60*60));

        StringBuilder builder = new StringBuilder();
        if(hours > 0) builder.append(String.format("%02d", hours)).append(":");
        builder.append(String.format("%02d", min)).append(":");
        builder.append(String.format("%02d", sec));

        return builder.toString();
    }

    public static Dimension fitIntoBounds(ReadableDimension toFit, ReadableDimension bounds) {
        int width = toFit.getWidth();
        int height = toFit.getHeight();

        float w = (float) width / bounds.getWidth();
        float h = (float) height / bounds.getHeight();

        if (w > h) {
            height = (int) (height / w);
            width = (int) (width / w);
        } else {
            height = (int) (height / h);
            width = (int) (width / h);
        }

        return new Dimension(width, height);
    }

    public static boolean isValidEmailAddress(String mail) {
        return mail.matches("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");
    }

    private static final PercentEscaper REPLAY_NAME_ENCODER = new PercentEscaper(".-_ ", false);

    public static String replayNameToFileName(String replayName) {
        return REPLAY_NAME_ENCODER.escape(replayName) + ".mcpr";
    }

    public static String fileNameToReplayName(String fileName) {
        String baseName = FilenameUtils.getBaseName(fileName);
        try {
            return URLDecoder.decode(baseName, Charsets.UTF_8.name());
        } catch (IllegalArgumentException e) {
            return baseName;
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }

    public static ResourceLocation getResourceLocationForPlayerUUID(UUID uuid) {
        //#if MC>=10800
        NetworkPlayerInfo info = getMinecraft().getNetHandler().getPlayerInfo(uuid);
        ResourceLocation skinLocation;
        if (info != null && info.hasLocationSkin()) {
            skinLocation = info.getLocationSkin();
        } else {
            skinLocation = DefaultPlayerSkin.getDefaultSkin(uuid);
        }
        return skinLocation;
        //#else
        //$$ EntityPlayer player = Minecraft.getMinecraft().theWorld.getPlayerEntityByUUID(uuid);
        //$$ if (player != null || !(player instanceof AbstractClientPlayer)) {
        //$$     return AbstractClientPlayer.locationStevePng;
        //$$ }
        //$$ return ((AbstractClientPlayer) player).getLocationSkin();
        //#endif
    }

    public static boolean isCtrlDown() {
        //#if MC>=11400
        //$$ return Screen.hasControlDown();
        //#else
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        //#endif
    }

    public static <T> void addCallback(ListenableFuture<T> future, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Futures.addCallback(future, new FutureCallback<T>() {
            @Override
            public void onSuccess(@Nullable T result) {
                onSuccess.accept(result);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                onFailure.accept(t);
            }
        });
    }

    public static GuiInfoPopup error(Logger logger, GuiContainer container, CrashReport crashReport, Runnable onClose) {
        // Convert crash report to string
        String crashReportStr = crashReport.getCompleteReport();

        // Log via logger
        logger.error(crashReportStr);

        // Try to save the crash report
        if (crashReport.getFile() == null) {
            try {
                File folder = new File(getMinecraft().mcDataDir, "crash-reports");
                File file = new File(folder, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-client.txt");
                logger.debug("Saving crash report to file: {}", file);
                crashReport.saveToFile(file);
            } catch (Throwable t) {
                logger.error("Saving crash report file:", t);
            }
        } else {
            logger.debug("Not saving crash report as file already exists: {}", crashReport.getFile());
        }

        logger.trace("Opening crash report popup GUI");
        GuiCrashReportPopup popup = new GuiCrashReportPopup(container, crashReportStr);
        Futures.addCallback(popup.getFuture(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                logger.trace("Crash report popup closed");
                if (onClose != null) {
                    onClose.run();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                logger.error("During error popup:", t);
            }
        });
        return popup;
    }

    private static class GuiCrashReportPopup extends GuiInfoPopup {
        private final GuiScrollable scrollable;

        public GuiCrashReportPopup(GuiContainer container, String crashReport) {
            super(container);
            setBackgroundColor(Colors.DARK_TRANSPARENT);

            // Add crash report to scrollable info
            getInfo().addElements(new VerticalLayout.Data(0.5),
                    new GuiLabel().setColor(Colors.BLACK).setI18nText("replaymod.gui.unknownerror"),
                    scrollable = new GuiScrollable().setScrollDirection(AbstractGuiScrollable.Direction.VERTICAL)
                            .setLayout(new VerticalLayout().setSpacing(2))
                            .addElements(null,Arrays.stream(crashReport.replace("\t", "    ").split("\n")).map(
                                    l -> new GuiLabel().setText(l).setColor(Colors.BLACK)).toArray(GuiElement[]::new)));

            // Replace close button with panel containing close and copy buttons
            GuiButton copyToClipboardButton = new GuiButton().setI18nLabel("chat.copy").onClick(() ->
                    MCVer.setClipboardString(crashReport)).setSize(150, 20);
            GuiButton closeButton = getCloseButton();
            popup.removeElement(closeButton);
            popup.addElements(new VerticalLayout.Data(1),
                    new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5)).setSize(305, 20)
                            .addElements(null, copyToClipboardButton, closeButton));

            // And open it
            open();
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            // Re-size the scrollable containing the crash report to 3/4 of the screen
            scrollable.setSize(size.getWidth() * 3 / 4, size.getHeight() * 3 / 4);
            super.draw(renderer, size, renderInfo);
        }
    }

    public static <T extends Throwable> void throwIfInstanceOf(Throwable t, Class<T> cls) throws T {
        if (cls.isInstance(t)) {
            throw cls.cast(t);
        }
    }
    public static void throwIfUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        }
    }

    public static void denyIfMinimalMode(GuiContainer container, Runnable onPopupClosed, Runnable orElseRun) {
        if (isNotMinimalModeElsePopup(container, onPopupClosed)) {
            orElseRun.run();
        }
    }

    public static boolean ifMinimalModeDoPopup(GuiContainer container, Runnable onPopupClosed) {
        return !isNotMinimalModeElsePopup(container, onPopupClosed);
    }

    public static boolean isNotMinimalModeElsePopup(GuiContainer container, Runnable onPopupClosed) {
        if (!ReplayMod.isMinimalMode()) {
            LOGGER.trace("Minimal mode not active, continuing");
            return true;
        }
        LOGGER.trace("Minimal mode active, denying action, opening popup");

        MinimalModeUnsupportedPopup popup = new MinimalModeUnsupportedPopup(container);
        Futures.addCallback(popup.getFuture(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOGGER.trace("Minimal mode popup closed");
                if (onPopupClosed != null) {
                    onPopupClosed.run();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("During minimal mode popup:", t);
            }
        });
        return false;
    }

    private static class MinimalModeUnsupportedPopup extends GuiInfoPopup {
        private MinimalModeUnsupportedPopup(GuiContainer container) {
            super(container);
            setBackgroundColor(Colors.DARK_TRANSPARENT);

            ProtocolVersion latestVersion = ProtocolVersion.getProtocols()
                    .stream()
                    .max(Comparator.comparing(ProtocolVersion::getId))
                    .orElseThrow(RuntimeException::new);
            getInfo().addElements(new VerticalLayout.Data(0.5),
                    new GuiLabel()
                            .setColor(Colors.BLACK)
                            .setI18nText("replaymod.gui.minimalmode.unsupported"),
                    new GuiLabel()
                            .setColor(Colors.BLACK)
                            .setI18nText("replaymod.gui.minimalmode.supportedversion",
                                    "1.7.10 - " + latestVersion.getName()));

            open();
        }
    }

    public static <T> T configure(T instance, Consumer<T> configure) {
        configure.accept(instance);
        return instance;
    }
}
