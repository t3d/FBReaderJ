/*
 * Copyright (C) 2010-2011 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.network;

import java.util.Map;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;

import org.geometerplus.zlibrary.core.network.ZLNetworkException;

import org.geometerplus.fbreader.network.*;
import org.geometerplus.fbreader.network.authentication.NetworkAuthenticationManager;
import org.geometerplus.fbreader.network.tree.NetworkBookTree;

import org.geometerplus.android.util.PackageUtil;

abstract class Util implements UserRegistrationConstants {
	private static final String REGISTRATION_ACTION =
		"android.fbreader.action.NETWORK_LIBRARY_REGISTER";
	static final String SMS_TOPUP_ACTION =
		"android.fbreader.action.NETWORK_LIBRARY_SMS_REFILLING";
	static final String CREDIT_CARD_TOPUP_ACTION =
		"android.fbreader.action.NETWORK_LIBRARY_CREDIT_CARD_TOPUP";
	static final String SELF_SERVICE_KIOSK_TOPUP_ACTION =
		"android.fbreader.action.NETWORK_LIBRARY_SELF_SERVICE_KIOSK_TOPUP";

	private static boolean testService(Activity activity, String action, String url) {
		return url != null && PackageUtil.canBeStarted(activity, new Intent(action, Uri.parse(url)), true);
	}

	static boolean isRegistrationSupported(Activity activity, INetworkLink link) {
		return testService(
			activity,
			REGISTRATION_ACTION,
			link.getUrlInfo(INetworkLink.URL_SIGN_UP).URL
		);
	}

	static void runRegistrationDialog(Activity activity, INetworkLink link) {
		try {
			final Intent intent = new Intent(
				REGISTRATION_ACTION,
				Uri.parse(link.getUrlInfo(INetworkLink.URL_SIGN_UP).URL)
			);
			if (PackageUtil.canBeStarted(activity, intent, true)) {
				activity.startActivityForResult(new Intent(
					REGISTRATION_ACTION,
					Uri.parse(link.getUrlInfo(INetworkLink.URL_SIGN_UP).URL)
				), USER_REGISTRATION_REQUEST_CODE);
			}
		} catch (ActivityNotFoundException e) {
		}
	}

	static void runAfterRegistration(NetworkAuthenticationManager mgr, Intent data) throws ZLNetworkException {
		final String userName = data.getStringExtra(USER_REGISTRATION_USERNAME);
		final String litresSid = data.getStringExtra(USER_REGISTRATION_LITRES_SID);
		mgr.initUser(userName, litresSid);
		if (userName.length() > 0 && litresSid.length() > 0) {
			try {
				mgr.initialize();
			} catch (ZLNetworkException e) {
				mgr.logOut();
				throw e;
			}
		}
	}

	static boolean isTopupSupported(Activity activity, INetworkLink link) {
		return
			isBrowserTopupSupported(activity, link) ||
			isTopupSupported(activity, link, SMS_TOPUP_ACTION) ||
			isTopupSupported(activity, link, CREDIT_CARD_TOPUP_ACTION) ||
			isTopupSupported(activity, link, SELF_SERVICE_KIOSK_TOPUP_ACTION);
	}

	static boolean isTopupSupported(Activity activity, INetworkLink link, String action) {
		return testService(
			activity,
			action,
			link.getUrlInfo(INetworkLink.URL_MAIN).URL
		);
	}

	static void runTopupDialog(Activity activity, INetworkLink link, String action) {
		try {
			final Intent intent = new Intent(
				action,
				Uri.parse(link.getUrlInfo(INetworkLink.URL_MAIN).URL)
			);
			final NetworkAuthenticationManager mgr = link.authenticationManager();
			if (mgr != null) {
				for (Map.Entry<String,String> entry : mgr.getTopupData().entrySet()) {
					intent.putExtra(entry.getKey(), entry.getValue());
				}
			}
			if (PackageUtil.canBeStarted(activity, intent, true)) {
				activity.startActivity(intent);
			}
		} catch (ActivityNotFoundException e) {
		}
	}

	static boolean isBrowserTopupSupported(Activity activity, INetworkLink link) {
		return link.getUrlInfo(INetworkLink.URL_TOPUP).URL != null;
	}

	static void openInBrowser(Context context, String url) {
		if (url != null) {
			url = NetworkLibrary.Instance().rewriteUrl(url, true);
			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		}
	}

	private static final String TREE_KEY_KEY = "org.geometerplus.android.fbreader.network.TreeKey";

	static void openTree(Context context, NetworkTree tree) {
		final Class<?> clz = tree instanceof NetworkBookTree
			? NetworkBookInfoActivity.class : NetworkCatalogActivity.class;
		context.startActivity(
			new Intent(context.getApplicationContext(), clz)
				.putExtra(TREE_KEY_KEY, tree.getUniqueKey())
		);
	}

	public static NetworkTree getTreeFromIntent(Intent intent) {
		final NetworkLibrary library = NetworkLibrary.Instance();
		final NetworkTree.Key key = (NetworkTree.Key)intent.getSerializableExtra(TREE_KEY_KEY);
		return library.getTreeByKey(key);
	}
}
