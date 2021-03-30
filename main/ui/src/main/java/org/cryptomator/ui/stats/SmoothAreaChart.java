package org.cryptomator.ui.stats;

import javafx.beans.NamedArg;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

// inspired by http://fxexperience.com/2012/01/curve-fitting-and-styling-areachart/
public class SmoothAreaChart extends AreaChart<Number, Number> {

	public SmoothAreaChart(@NamedArg("xAxis") Axis<Number> xAxis, @NamedArg("yAxis") Axis<Number> yAxis) {
		super(xAxis, yAxis);
	}

	@Override
	protected void layoutPlotChildren() {
		super.layoutPlotChildren();
		var iter = getDisplayedSeriesIterator();
		while (iter.hasNext()) {
			var series = iter.next();
			final Path seriesLine = (Path)((Group)series.getNode()).getChildren().get(1);
			final Path fillPath = (Path)((Group)series.getNode()).getChildren().get(0);
			var dx = getXAxis().getWidth() / series.getData().size();
			smooth(seriesLine.getElements(), fillPath.getElements(), dx);
		}
	}

	private static void smooth(ObservableList<PathElement> strokeElements, ObservableList<PathElement> fillElements, double dx) {
		// as we do not have direct access to the data, first recreate the list of all the data points we have
		final Point2D[] dataPoints = new Point2D[strokeElements.size()];
		for (int i = 0; i < strokeElements.size(); i++) {
			final PathElement element = strokeElements.get(i);
			if (element instanceof MoveTo) {
				final MoveTo move = (MoveTo)element;
				dataPoints[i] = new Point2D(move.getX(), move.getY());
			} else if (element instanceof LineTo) {
				final LineTo line = (LineTo)element;
				final double x = line.getX(), y = line.getY();
				dataPoints[i] = new Point2D(x, y);
			}
		}
		// next we need to know the zero Y value
		final double zeroY = ((MoveTo) fillElements.get(0)).getY();
		final double dx2 = dx / 2.0;

		// now clear and rebuild elements
		strokeElements.clear();
		fillElements.clear();
		// start both paths
		strokeElements.add(new MoveTo(dataPoints[0].getX(),dataPoints[0].getY()));
		fillElements.add(new MoveTo(dataPoints[0].getX(),zeroY));
		fillElements.add(new LineTo(dataPoints[0].getX(),dataPoints[0].getY()));
		// add curves
		for (int i = 1; i < dataPoints.length; i++) {
			final int ci = i-1;
			strokeElements.add(new CubicCurveTo(
					dataPoints[ci].getX() + dx2, dataPoints[ci].getY(),
					dataPoints[i].getX() - dx2, dataPoints[i].getY(),
					dataPoints[i].getX(), dataPoints[i].getY()));
			fillElements.add(new CubicCurveTo(
					dataPoints[ci].getX() + dx2, dataPoints[ci].getY(),
					dataPoints[i].getX() - dx2, dataPoints[i].getY(),
					dataPoints[i].getX(),dataPoints[i].getY()));
		}
		// end the paths
		fillElements.add(new LineTo(dataPoints[dataPoints.length-1].getX(),zeroY));
		fillElements.add(new ClosePath());
	}

}
