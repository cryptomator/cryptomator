package org.cryptomator.ui.addvaultwizard;

import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class ReadmeGenerator {

	// specs: https://web.archive.org/web/20190708132914/http://www.kleinlercher.at/tools/Windows_Protocols/Word2007RTFSpec9.pdf
	private static final String RTF_HEADER = "{\\rtf1\\fbidis\\ansi\\uc0\\fs32\n";
	private static final String RTF_FOOTER = "}";
	private static final String HEADING = "\\fs40\\qc %s";
	private static final String EMPTY_PAR = "";
	private static final String DONT_PAR = "\\b %s";
	private static final String IDENT_PAR = "    %s";
	private static final String HELP_URL = "{\\field{\\*\\fldinst HYPERLINK \"http://docs.cryptomator.org/\"}{\\fldrslt http://docs.cryptomator.org}}";

	private final ResourceBundle resourceBundle;

	@Inject
	public ReadmeGenerator(ResourceBundle resourceBundle) {
		this.resourceBundle = resourceBundle;
	}

	public String createVaultStorageLocationReadmeRtf() {
		return createDocument(List.of( //
				String.format(HEADING, resourceBundle.getString("addvault.new.readme.storageLocation.1")), //
				resourceBundle.getString("addvault.new.readme.storageLocation.2"), //
				EMPTY_PAR, //
				String.format(DONT_PAR, resourceBundle.getString("addvault.new.readme.storageLocation.3")),  //
				String.format(IDENT_PAR, resourceBundle.getString("addvault.new.readme.storageLocation.4")),  //
				String.format(IDENT_PAR, resourceBundle.getString("addvault.new.readme.storageLocation.5")),  //
				EMPTY_PAR, //
				resourceBundle.getString("addvault.new.readme.storageLocation.6"),  //
				String.format(IDENT_PAR, resourceBundle.getString("addvault.new.readme.storageLocation.7")),  //
				String.format(IDENT_PAR, resourceBundle.getString("addvault.new.readme.storageLocation.8")),  //
				String.format(IDENT_PAR, resourceBundle.getString("addvault.new.readme.storageLocation.9")),  //
				EMPTY_PAR, //
				String.format(resourceBundle.getString("addvault.new.readme.storageLocation.10"), HELP_URL)  //
		));
	}

	public String createVaultAccessLocationReadmeRtf() {
		return createDocument(List.of( //
				String.format(HEADING, resourceBundle.getString("addvault.new.readme.accessLocation.1")), //
				resourceBundle.getString("addvault.new.readme.accessLocation.2"), //
				EMPTY_PAR, //
				resourceBundle.getString("addvault.new.readme.accessLocation.3"), //
				EMPTY_PAR, //
				resourceBundle.getString("addvault.new.readme.accessLocation.4")));
	}

	@VisibleForTesting
	String createDocument(Iterable<String> paragraphs) {
		StringBuilder sb = new StringBuilder(RTF_HEADER);
		for (String p : paragraphs) {
			sb.append("{\\sa80 ");
			appendEscaped(sb, p);
			sb.append("}\\par \n");
		}
		sb.append(RTF_FOOTER);
		return sb.toString();
	}

	@VisibleForTesting
	String escapeNonAsciiChars(CharSequence input) {
		StringBuilder sb = new StringBuilder();
		appendEscaped(sb, input);
		return sb.toString();
	}

	private void appendEscaped(StringBuilder sb, CharSequence input) {
		input.chars().forEachOrdered(c -> {
			if (c < 128) {
				sb.append((char) c);
			} else if (c <= 0xFF) {
				sb.append("\\'").append(String.format("%02X", c));
			} else if (c < 0xFFFF) {
				sb.append("\\uc1\\u").append(c);
			}
		});
	}


}
