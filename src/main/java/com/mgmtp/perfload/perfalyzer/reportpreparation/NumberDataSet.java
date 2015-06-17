/*
 * Copyright (c) 2013-2015 mgm technology partners GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mgmtp.perfload.perfalyzer.reportpreparation;

import static com.google.common.collect.Lists.newArrayListWithExpectedSize;

import java.util.Collections;
import java.util.List;

import org.jfree.data.DomainOrder;
import org.jfree.data.general.AbstractSeriesDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.IntervalXYDelegate;
import org.jfree.data.xy.XYDataset;

/**
 * {@link XYDataset} implementation for lists of lists of Double objects.
 *
 * @author rnaegele
 */
public class NumberDataSet extends AbstractSeriesDataset implements IntervalXYDataset {

	private final List<NameSeriesWrapper> seriesList = newArrayListWithExpectedSize(5);
	private final IntervalXYDelegate intervalDelegate = new IntervalXYDelegate(this);

	/**
	 * Adds a series to the dataset.
	 *
	 * @param name   the series name
	 * @param series the seris data
	 */
	public void addSeries(final String name, final List<SeriesPoint> series) {
		seriesList.add(new NameSeriesWrapper(name, series));
	}

	public void addSeriesPoint(final String name, final SeriesPoint point) {
		for (NameSeriesWrapper wrapper : seriesList) {
			if (wrapper.name.equals(name)) {
				List<SeriesPoint> seriesPoints = wrapper.series;
				seriesPoints.add(point);
				return;
			}
		}

		List<SeriesPoint> seriesPoints = newArrayListWithExpectedSize(100);
		seriesPoints.add(point);
		NameSeriesWrapper wrapper = new NameSeriesWrapper(name, seriesPoints);
		seriesList.add(wrapper);
	}

	public boolean isEmpty() {
		for (NameSeriesWrapper wrapper : seriesList) {
			if (!wrapper.series.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Sorts the list of series by their name.
	 */
	public void sortSeries() {
		Collections.sort(seriesList);
	}

	@Override
	public DomainOrder getDomainOrder() {
		return DomainOrder.NONE;
	}

	@Override
	public int getItemCount(final int series) {
		return seriesList.get(series).series.size();
	}

	@Override
	public Number getX(final int series, final int item) {
		return seriesList.get(series).series.get(item).getX();
	}

	@Override
	public double getXValue(final int series, final int item) {
		return getX(series, item).doubleValue();
	}

	@Override
	public Number getY(final int series, final int item) {
		return seriesList.get(series).series.get(item).getY();
	}

	@Override
	public double getYValue(final int series, final int item) {
		return getY(series, item).doubleValue();
	}

	@Override
	public int getSeriesCount() {
		return seriesList.size();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Comparable<String> getSeriesKey(final int series) {
		return seriesList.get(series).name;
	}

	public static class SeriesPoint {
		private final Number x;
		private final Number y;

		public SeriesPoint(final Number x, final Number y) {
			this.x = x;
			this.y = y;
		}

		/**
		 * @return the x
		 */
		public Number getX() {
			return x;
		}

		/**
		 * @return the y
		 */
		public Number getY() {
			return y;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (x == null ? 0 : x.hashCode());
			result = prime * result + (y == null ? 0 : y.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			SeriesPoint other = (SeriesPoint) obj;
			if (x == null) {
				if (other.x != null) {
					return false;
				}
			} else if (!x.equals(other.x)) {
				return false;
			}
			if (y == null) {
				if (other.y != null) {
					return false;
				}
			} else if (!y.equals(other.y)) {
				return false;
			}
			return true;
		}
	}

	/**
	 * Wraps series and series name for sorting.
	 *
	 * @author rnaegele
	 */
	static class NameSeriesWrapper implements Comparable<NameSeriesWrapper> {
		String name;
		List<SeriesPoint> series;

		public NameSeriesWrapper(final String name, final List<SeriesPoint> series) {
			this.name = name;
			this.series = series;
		}

		@Override
		public int compareTo(final NameSeriesWrapper other) {
			return name.compareTo(other.name);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (name == null ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			NameSeriesWrapper other = (NameSeriesWrapper) obj;
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}
	}

	@Override
	public Number getStartX(final int series, final int item) {
		return intervalDelegate.getStartX(series, item);
	}

	@Override
	public double getStartXValue(final int series, final int item) {
		return intervalDelegate.getStartXValue(series, item);
	}

	@Override
	public Number getEndX(final int series, final int item) {
		return intervalDelegate.getEndX(series, item);
	}

	@Override
	public double getEndXValue(final int series, final int item) {
		return intervalDelegate.getEndXValue(series, item);
	}

	@Override
	public Number getStartY(final int series, final int item) {
		return getY(series, item);
	}

	@Override
	public double getStartYValue(final int series, final int item) {
		return getYValue(series, item);
	}

	@Override
	public Number getEndY(final int series, final int item) {
		return getY(series, item);
	}

	@Override
	public double getEndYValue(final int series, final int item) {
		return getYValue(series, item);
	}
}
