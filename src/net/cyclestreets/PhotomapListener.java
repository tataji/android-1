package net.cyclestreets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cyclestreets.api.Photo;

import org.andnav.osm.events.MapAdapter;
import org.andnav.osm.events.ScrollEvent;
import org.andnav.osm.events.ZoomEvent;
import org.andnav.osm.util.BoundingBoxE6;
import org.andnav.osm.views.OpenStreetMapView;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class PhotomapListener extends MapAdapter {
	public Map<Integer,Photo> photoMap = new HashMap<Integer,Photo>();
	
	protected OpenStreetMapView map;
	protected List<PhotoItem> photoList;
	protected PhotoMarkers photoMarkers;

	public PhotomapListener(Context ctx, OpenStreetMapView map, List<PhotoItem> photoList) {
		this.map = map;
		this.photoList = photoList;
		this.photoMarkers = new PhotoMarkers(ctx.getResources());
	}
	
	@Override
	public boolean onScrollStart(ScrollEvent event) {
		int x = event.getX();
		int y = event.getY();
		Log.i(getClass().getSimpleName(), "Starting scroll to: " + x + "," + y);
		
		refreshPhotos();
		return true;
	}
	
	@Override
	public boolean onZoomStart(ZoomEvent event) {
		int z = event.getZoomLevel();
		Log.i(getClass().getSimpleName(), "Starting zoom to: " + z);

		// clear photos for new zoom level
		photoList.clear();
		refreshPhotos();
		return true;
	}

	@Override
	public boolean onScrollFinish(ScrollEvent event) {
		int x = event.getX();
		int y = event.getY();
		Log.i(getClass().getSimpleName(), "Finished scroll to: " + x + "," + y);
		return true;
	}
	
	@Override
	public boolean onZoomFinish(ZoomEvent event) {
		int z = event.getZoomLevel();
		Log.i(getClass().getSimpleName(), "Finished zoom to: " + z);
		return true;
	}

	protected void refreshPhotos() {
		BoundingBoxE6 bounds = map.getVisibleBoundingBoxE6();
		double n = bounds.getLatNorthE6() / 1E6;
		double s = bounds.getLatSouthE6() / 1E6;
		double e = bounds.getLonEastE6() / 1E6;
		double w = bounds.getLonWestE6() / 1E6;
		Log.i(getClass().getSimpleName(), "Bounding box: " + n + " " + s + " " + e + " " + w);
		
		int zoom = map.getZoomLevel();
		double clat = map.getMapCenterLatitudeE6() / 1E6;
		double clon = map.getMapCenterLongitudeE6() / 1E6;
		new GetPhotosTask().execute(clat, clon, zoom, n, s, e, w);		
	}

	private class GetPhotosTask extends AsyncTask<Object,Void,List<Photo>> {
		protected List<Photo> doInBackground(Object... params) {
			double clat = (Double) params[0];
			double clon = (Double) params[1];
			int zoom = (Integer) params[2];
			double n = (Double) params[3];
			double s = (Double) params[4];
			double e = (Double) params[5];
			double w = (Double) params[6];
			List<Photo> photos;
			try {
				// TODO: do incremental processing of photos
				// TODO: reset photos when zoom level changes
				photos = CycleStreets.apiClient.getPhotos(clat, clon, zoom, n, s, e, w);
				Log.d(getClass().getSimpleName(), "got photos: " + photos.size());
				if (!photos.isEmpty()) {
					Log.d(getClass().getSimpleName(), photos.get(0).caption);
				}
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			return photos;
		}
		
		@Override
		protected void onPostExecute(List<Photo> photos) {
			Log.d(getClass().getSimpleName(), "photolist contains: [" + photoList.size() + "] " + photoList);
			Log.d(getClass().getSimpleName(), "photos contains: [" + photos.size() + "] " + photos);
			for (Photo photo: photos) {
				// TODO check for duplicates
				photoList.add(new PhotoItem(photo, photoMarkers));
			}
			Log.d(getClass().getSimpleName(), "photolist contains: [" + photoList.size() + "] " + photoList);

			// force map redraw
			Log.d(getClass().getSimpleName(), "invalidating map");
			map.postInvalidate();
		}
	}
}