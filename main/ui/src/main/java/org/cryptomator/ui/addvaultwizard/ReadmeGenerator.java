package org.cryptomator.ui.addvaultwizard;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class ReadmeGenerator {
	
	// specs: https://web.archive.org/web/20190708132914/http://www.kleinlercher.at/tools/Windows_Protocols/Word2007RTFSpec9.pdf
	private static final String RTF_HEADER = "{\\rtf1\\fbidis\\ansi\\uc0\\fs32\n";
	private static final String RTF_FOOTER = "}";
	private static final String HELP_URL = "{\\field{\\*\\fldinst HYPERLINK \"http://www.google.com/\"}{\\fldrslt google.com}}";

	private final ResourceBundle resourceBundle;

	@Inject
	public ReadmeGenerator(ResourceBundle resourceBundle){
		this.resourceBundle = resourceBundle;
	}
	
	public String createVaultStorageLocationReadmeRtf() {
		return createDocument(List.of( //
				resourceBundle.getString("addvault.new.readme.storageLocation.1"), //
				resourceBundle.getString("addvault.new.readme.storageLocation.2"), //
				resourceBundle.getString("addvault.new.readme.storageLocation.3"),  //
				String.format(resourceBundle.getString("addvault.new.readme.storageLocation.4"), HELP_URL)  //
		));
	}

	// visible for testing
	String createDocument(Iterable<String> paragraphs) {
		StringBuilder sb = new StringBuilder(RTF_HEADER);
		for (String p : paragraphs) {
			sb.append("\\par {\\sa80 ");
			appendEscaped(sb, p);
			sb.append("}\n");
		}
		sb.append(RTF_FOOTER);
		return sb.toString();
	}
	
	// visible for testing
	String escapeNonAsciiChars(CharSequence input) {
		StringBuilder sb = new StringBuilder();
		appendEscaped(sb, input);
		return sb.toString();
	}
	
	private void appendEscaped(StringBuilder sb, CharSequence input) {
		input.chars().forEachOrdered(c -> {
			if (c < 128) {
				sb.append((char) c);
			} else if (c < 0xFFFF) {
				sb.append("\\u").append(c);
			}
		});
	}
	
	

}
