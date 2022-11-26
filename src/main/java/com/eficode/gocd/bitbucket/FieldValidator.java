package com.eficode.gocd.bitbucket;

import java.util.Map;

public interface FieldValidator {
	public void validate(Map<String, Object> fieldValidation);
}
