/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.util.Comparator;

import org.apache.commons.collections4.CollectionUtils;

/**
 * Comparator used to separate and order node properties using the dependencies.
 */
class DependencyComparator implements Comparator<ParameterVo> {

	@Override
	public int compare(final ParameterVo arg0, final ParameterVo arg1) {
		var compare = arg0.getId().compareTo(arg1.getId());

		// THIS -> OTHER dependency
		if (CollectionUtils.isNotEmpty(arg0.getDepends())) {
			compare += 10;
			if (arg0.getDepends().contains(arg1.getId())) {
				compare += 10;
			}
		}

		// OTHER -> THIS dependency
		if (CollectionUtils.isNotEmpty(arg1.getDepends())) {
			compare -= 10;
			if (arg1.getDepends().contains(arg0.getId())) {
				compare -= 10;
			}
		}
		return compare;
	}

}
