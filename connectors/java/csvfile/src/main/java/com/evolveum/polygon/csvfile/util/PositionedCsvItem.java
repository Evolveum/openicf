package com.evolveum.polygon.csvfile.util;

import java.util.List;

/**
 * @author mederly
 */
public class PositionedCsvItem extends CsvItem {

	private final int position;

	public PositionedCsvItem(List<String> attributes, int position) {
		super(attributes);
		this.position = position;
	}

	public int getPosition() {
		return position;
	}
}
