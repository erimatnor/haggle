package vendetta.vendettagui.shapes;
import vendetta.Vendetta;
import javax.media.j3d.*;
import javax.vecmath.Vector3f;
import vendetta.visualization.shapes.*;
import vendetta.util.log.Log;

public class LookUpLines extends javax.media.j3d.BranchGroup {
	public static final Log LOG = Log.getInstance("Haggle");

	private final int MAX_P = 200;

	private TransformGroup tg;
	private Transform3D trans;
	public String seq = "0";
	public String key = "0";
	public ColoringAttributes color;
	private Appearance app;
	private Shape3D shape;
	private LineStripArray lines;
	
	/** The coords for the lines, 2 * nodes - 1 */
	private int tot_num=0;
	private int strip_count[] = { 2 };
	private int line_num = 0;
	
	// 
	private float offset;
	
	/** Creates a new lookup line, starting at startNode */
	public LookUpLines( MonitorNodeShape startNode, String seq, String key,
			float[] col, float offset ) {
		super();
		
		this.offset = offset;
		this.seq = seq;
		this.key = key;
		
		tg = new TransformGroup();
		trans = new Transform3D();
		app = new Appearance();
		color = new ColoringAttributes( col[ 0 ], col[ 1 ], col[ 2 ],
			   ColoringAttributes.FASTEST );
		addChild( tg );
		app.setColoringAttributes( color );
		setPickable( false );
		setCapability( ALLOW_DETACH );
		// we have 1 points in the first line
		lines = new LineStripArray( 
				MAX_P, GeometryArray.COORDINATES, strip_count );
		
		lines.setCapability( lines.ALLOW_COORDINATE_WRITE );
		lines.setCapability( lines.ALLOW_COUNT_WRITE );	
		
		// Draw a line 
		float[] pos_src = startNode.getPosition();
		pos_src[ 2 ] += offset;
		lines.setCoordinate( tot_num++, pos_src );
		pos_src[ 0 ] *= 1.2f;
		pos_src[ 1 ] *= 1.2f;
		lines.setCoordinate( tot_num++, pos_src );
		
		LineAttributes la = new LineAttributes();
		la.setLineAntialiasingEnable( true );
		la.setLineWidth( 1.7f );
		app.setLineAttributes( la );
		shape = new Shape3D( lines, app );
		addChild( shape );
		
		shape.setPickable( false );
	}
	
	private void increase_stripcount() {
		int[] temp = strip_count;
		strip_count = new int[ temp.length + 1 ];
		for( int i = 0; i < temp.length; i++ )
			strip_count[ i ] = temp[ i ];
	}

	public void addStraightLine( MonitorNodeShape src, MonitorNodeShape dest ) {
		if( src == null || dest == null ) {
				LOG.warn( "(LookUpLine)" + "Skipping a line because" +
					" the shape was not found." );
				return;
		}
		line_num++;
		int num = 0;
		float[] pos_src = src.getPosition();
		float[] pos_dest = dest.getPosition();
		if( tot_num + 1 >= MAX_P ) return;
		lines.setCoordinate( tot_num++, pos_src );
		num++;
		if( tot_num + 1 >= MAX_P ) return;
		lines.setCoordinate( tot_num++, pos_dest );
		num++;
		increase_stripcount();
		strip_count[ line_num ] = num;
		lines.setStripVertexCounts( strip_count );
	}

	public void addLine( MonitorNodeShape src, MonitorNodeShape dest ) {
		try {
			if( src == null || dest == null ) {
				LOG.warn( "(LookUpLine)" + "Skipping a line because" +
					" the shape was not found." );
				return;
			}
			line_num++;
			int num = 0;
			// did we have a gap
			float[] pos_src = src.getPosition();
			pos_src[ 2 ] += offset;
			float[] pos_dest = dest.getPosition();
			float[] pos_var = new float[ 3 ];
			pos_var[ 0 ] = pos_src[ 0 ];
			pos_var[ 1 ] = pos_src[ 1 ];
			pos_var[ 2 ] = pos_src[ 2 ];
			
			float dist = (float)Math.sqrt( 
					(pos_dest[ 0 ]-pos_src[ 0 ])*(pos_dest[ 0 ]-pos_src[ 0 ]) +
					(pos_dest[ 1 ]-pos_src[ 1 ])*(pos_dest[ 1 ]-pos_src[ 1 ]) );
			// max_dist = 1.7
			float num_stepsf = dist * 15.0f;
			if( num_stepsf < 8.0f )
				num_stepsf = 8.0f;
			
			float dx = ( pos_dest[ 0 ] - pos_src[ 0 ] ) / num_stepsf;
			float dy = ( pos_dest[ 1 ] - pos_src[ 1 ] ) / num_stepsf;
			
			float delta = dist / num_stepsf;
			
			float k = 0.8f;
	//		if( dist < 0.4f )
	//			k = 2.4f;
			
			float m = dist / 2.0f;
			if( m < 0.1f ) {
		//		m = 0.1f;
			}
			m = (m*m)*k;
			if( tot_num + 1 >= MAX_P ) return;
			lines.setCoordinate( tot_num++, pos_src );
			num++;
			// draw the first part
			float i = -dist/2.0f;
			for( ; i < dist/2.0f; i += delta ) {
				pos_var[ 2 ] = m - k*i*i + offset;
				lines.setCoordinate( tot_num++, pos_var );
				num++;
				pos_var[ 0 ] = pos_var[ 0 ] + dx;
				pos_var[ 1 ] = pos_var[ 1 ] + dy;
				if( tot_num + 1 >= MAX_P ) return;
			}
			pos_dest[ 2 ] += offset;
			if( tot_num + 1 >= MAX_P ) return;
			lines.setCoordinate( tot_num++, pos_dest );
			num++;
			// increase the strip count
			increase_stripcount();
			strip_count[ line_num ] = num;
			lines.setStripVertexCounts( strip_count );
		} catch( Exception e ) {
			LOG.debug( "(LookUpLines)" + e.getMessage() );
			e.printStackTrace();
		}
	}
}
