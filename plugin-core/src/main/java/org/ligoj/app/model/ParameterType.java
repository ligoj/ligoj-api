/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

/**
 * Parameter types.
 */
public enum ParameterType {

	/**
	 * Free text.
	 */
	TEXT,

	/**
	 * Single selection.
	 */
	SELECT,

	/**
	 * Multiple selection.
	 */
	MULTIPLE,

	/**
	 * Number
	 */
	INTEGER,

	/**
	 * Date
	 */
	DATE,

	/**
	 * Boolean
	 */
	BOOL,

	/**
	 * Multiple free text.
	 */
	TAGS;

	/**
	 * Return true is this criteria is based on a fixed length choice.
	 *
	 * @return true is this criteria is based on a fixed length choice.
	 */
	public boolean isArray() {
		return this == ParameterType.SELECT || this == ParameterType.MULTIPLE;
	}
}
