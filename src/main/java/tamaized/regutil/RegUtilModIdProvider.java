package tamaized.regutil;

import tamaized.beanification.Component;

import javax.annotation.Nullable;
import java.util.Optional;

@Component
public class RegUtilModIdProvider {

	@Nullable
	private String modId;

	public void setModId(String modId) {
		this.modId = modId;
	}

	public Optional<String> getModId() {
		return Optional.ofNullable(modId);
	}

}
