package app.revanced.extension.dcinside.patches.layout.branding;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.Nullable;
import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.ResourceType;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.shared.patches.CustomBrandingPatch.BrandingTheme; // shared enum 재사용
import app.revanced.extension.shared.settings.BaseSettings; // shared 설정 재사용
import java.util.ArrayList;
import java.util.Locale;

public class DcinsideBrandingPatch {

    @Nullable
    private static Integer notificationSmallIcon;

    private static int numberOfPresetAppNames() {
        return 5;
    }

    private static boolean userProvidedCustomIcon() {
        return false; // returnEarly로 실제 값 주입됨
    }

    private static boolean userProvidedCustomName() {
        return false; // returnEarly로 실제 값 주입됨
    }

    public static int getDefaultAppNameIndex() {
        return userProvidedCustomName() ? numberOfPresetAppNames() : 1;
    }

    public static BrandingTheme getDefaultIconStyle() {
        return userProvidedCustomIcon() ? BrandingTheme.CUSTOM : BrandingTheme.ORIGINAL;
    }

    private static String toAlias(BrandingTheme theme, String packageName, int index) {
        if (index <= 0) {
            Logger.printException(() -> "App index starts at index 1");
            return null;
        }
        return packageName + ".revanced_" + theme.name().toLowerCase(Locale.US) + '_' + index;
    }

    private static int getNotificationSmallIcon() {
        if (notificationSmallIcon == null) {
            BrandingTheme theme = BaseSettings.CUSTOM_BRANDING_ICON.get();
            if (theme == BrandingTheme.ORIGINAL) {
                notificationSmallIcon = 0;
                return 0;
            }
            String resourceName = (theme == BrandingTheme.CUSTOM)
                    ? "revanced_notification_icon_custom"
                    : "revanced_notification_icon";
            notificationSmallIcon = Utils.getResourceIdentifier(ResourceType.DRAWABLE, resourceName);
            if (notificationSmallIcon == 0) {
                Logger.printException(() -> "Could not load notification small icon");
            }
        }
        return notificationSmallIcon;
    }

    public static void setNotificationIcon(Notification.Builder builder) {
        try {
            int icon = getNotificationSmallIcon();
            if (icon != 0) {
                builder.setSmallIcon(icon).setColor(0);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setNotificationIcon failure", ex);
        }
    }

    /**
     * GmsCoreSupport.isPackageNameOriginal() 체크는 제거했습니다.
     * 디시인사이드는 gmscore 우회가 필요 없으므로 루트 마운트 예외를 신경 쓸 필요가 없습니다.
     */
    public static void setBranding() {
        try {
            Context context = Utils.getContext();
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();

            BrandingTheme currentTheme = BaseSettings.CUSTOM_BRANDING_ICON.get();
            int currentNameIndex = BaseSettings.CUSTOM_BRANDING_NAME.get();

            ArrayList<ComponentName> toDisable = new ArrayList<>();
            ComponentName target = null;
            ComponentName fallback = null;

            for (BrandingTheme theme : BrandingTheme.values()) {
                for (int i = 1; i <= numberOfPresetAppNames(); i++) {
                    ComponentName alias = new ComponentName(packageName, toAlias(theme, packageName, i));
                    if (fallback == null) fallback = alias;

                    if (theme == currentTheme && i == currentNameIndex) {
                        target = alias;
                    } else {
                        toDisable.add(alias);
                    }
                }
            }

            if (target == null) {
                Utils.showToastLong("Custom branding reset");
                BaseSettings.CUSTOM_BRANDING_ICON.resetToDefault();
                BaseSettings.CUSTOM_BRANDING_NAME.resetToDefault();
                toDisable.remove(fallback);
                target = fallback;
            }

            for (ComponentName alias : toDisable) {
                pm.setComponentEnabledSetting(alias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }

            final ComponentName finalTarget = target;
            Logger.printInfo(() -> "Enabling: " + finalTarget.getClassName());
            pm.setComponentEnabledSetting(target, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (Exception ex) {
            Logger.printException(() -> "setBranding failure", ex);
        }
    }
}