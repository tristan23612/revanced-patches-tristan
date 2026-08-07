package app.revanced.extension.dcinside.patches.post.view;

import app.revanced.extension.dcinside.settings.Settings;

public class HideBottomLikePostsPatch {
    public static boolean shouldHideBottomLikePosts() {
        return Settings.HIDE_BOTTOM_LIKE_POSTS.get();
    }
}
