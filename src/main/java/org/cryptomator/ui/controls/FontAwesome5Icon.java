package org.cryptomator.ui.controls;

/**
 * Inspired by de.jensd:fontawesomefx-fontawesome
 */
public enum FontAwesome5Icon {
	ANCHOR("\uF13D"), //
	ARROW_UP("\uF062"), //
	BAN("\uF05E"), //
	BUG("\uF188"), //
	CARET_DOWN("\uF0D7"), //
	CARET_RIGHT("\uF0Da"), //
	CHECK("\uF00C"), //
	CLOCK("\uF017"), //
	CLIPBOARD("\uF328"), //
	COG("\uF013"), //
	COGS("\uF085"), //
	COPY("\uF0C5"), //
	CROWN("\uF521"), //
	EDIT("\uF044"), //
	EXCHANGE_ALT("\uF362"), //
	EXCLAMATION("\uF12A"), //
	EXCLAMATION_CIRCLE("\uF06A"), //
	EXCLAMATION_TRIANGLE("\uF071"), //
	EYE("\uF06E"), //
	EYE_SLASH("\uF070"), //
	FAST_FORWARD("\uF050"), //
	FILE("\uF15B"), //
	FILE_DOWNLOAD("\uF56D"), //
	FILE_IMPORT("\uF56F"), //
	FOLDER_OPEN("\uF07C"), //
	FUNNEL("\uF0B0"), //
	HAND_HOLDING_HEART("\uF4BE"), //
	HEART("\uF004"), //
	HDD("\uF0A0"), //
	INFO("\uF129"), //
	INFO_CIRCLE("\uF05A"), //
	KEY("\uF084"), //
	LINK("\uF0C1"), //
	LOCK("\uF023"), //
	LOCK_OPEN("\uF3C1"), //
	MAGIC("\uF0D0"), //
	PENCIL("\uF303"), //
	PLUS("\uF067"), //
	PRINT("\uF02F"), //
	QUESTION("\uF128"), //
	QUESTION_CIRCLE("\uf059"), //
	REDO("\uF01E"), //
	SEARCH("\uF002"), //
	SPINNER("\uF110"), //
	STETHOSCOPE("\uF0f1"), //
	SYNC("\uF021"), //
	TIMES("\uF00D"), //
	TRASH("\uF1F8"), //
	UNLINK("\uf127"), //
	USER_COG("\uf4fe"), //
	WRENCH("\uF0AD"), //
	WINDOW_MINIMIZE("\uF2D1"), //
	;

	private final String unicode;

	FontAwesome5Icon(String unicode) {
		this.unicode = unicode;
	}

	public String unicode() {
		return unicode;
	}
}
