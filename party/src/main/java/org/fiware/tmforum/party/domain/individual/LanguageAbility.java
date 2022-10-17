package org.fiware.tmforum.party.domain.individual;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.TimePeriod;

@Data
@EqualsAndHashCode(callSuper = true)
public class LanguageAbility extends Entity {

	private boolean isFavouriteLanguage;
	private String languageCode;
	private String languageName;
	private String listeningProficiency;
	private String readingProficiency;
	private String speakingProficiency;
	private String writingProficiency;
	private TimePeriod validFor;
}
