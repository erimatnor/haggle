package vendetta.vendettagui.shapes;
import vendetta.*;
//import vendetta.overlays.bamboo.*;
import javax.media.j3d.*;
import javax.vecmath.Vector3f;
import vendetta.visualization.shapes.*;
import vendetta.util.log.Log;

public class RTLines extends javax.media.j3d.BranchGroup {
	public static final Log LOG = Log.getInstance("Haggle");

	private TransformGroup tg;
	private Transform3D trans;
	public ColoringAttributes color;
	private Appearance app;
	private Shape3D shape;
	private LineStripArray[] lines;
	private final ColoringAttributes beep = new ColoringAttributes( 
			0.0f, 1.0f, 0.0f, ColoringAttributes.FASTEST );

	/** Creates a new lookup line, starting at startNode */
	public RTLines( MonitorNodeShape src, String[] RT ) {
		super();
		
		if( RT[ 3 ].equals( "empty" ) ) {
			LOG.debug( "Routing table empty." );
			return;
		}
				
		tg = new TransformGroup();
		trans = new Transform3D();
		app = new Appearance();
		color = new ColoringAttributes( 0.1f, 0.5f, 0.1f,
			   ColoringAttributes.FASTEST );
		addChild( tg );
		app.setColoringAttributes( color );
		setPickable( false );
		setCapability( ALLOW_DETACH );	
	
		LineAttributes la = new LineAttributes();
		la.setLineAntialiasingEnable( true );
		la.setLineWidth( 1.75f );
		app.setLineAttributes( la );
		
		lines = new LineStripArray[ (RT.length - 3) ];
		LOG.debug( "RT-size: " + (lines.length) );
		/*
		MonitorNode[] nodes = Vendetta.getMonitorNodes();
		MonitorNodeShape dest = null;
		
		int line = 0;
		for( int i = 3; i < RT.length; i++ ) {
			for( int n = 0; n < nodes.length; n++ ) {
				if( nodes[ n ] == null )
					break;
				if( ((BambooNode)nodes[ n ]).nodeID.equals( RT[ i ] ) ) {
					nodes[ n ].beep( beep );
					dest = nodes[ n ].getMonitorNodeShape( 0 );
					break;
				}
			}
			if( dest == null ) {
				LOG.debug( "NodeShape not found! (" + RT[ i ] + ")" );
				continue;
			}
				
			lines[ line ] = new LineStripArray( 
				30, GeometryArray.COORDINATES, new int[] { 2 } );
		//	lines.setCapability( lines.ALLOW_COORDINATE_WRITE );
		//	lines.setCapability( lines.ALLOW_COUNT_WRITE );	
			
			addLine( lines[ line ], src, dest );
	
			shape = new Shape3D( lines[ line ], app );
			addChild( shape );
		
			shape.setPickable( false );
			
			line++;
		}	*/
	}

	private void addLine( LineStripArray line, MonitorNodeShape src, MonitorNodeShape dest ) {
		try {
			int num = 0;
			float[] pos_src = src.getPosition();
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
			
			float m = dist / 2.0f;
			if( m < 0.1f ) {
				m = 0.1f;
			}
			m = (m*m)*k;
			
			line.setCoordinate( num++, pos_src );
			
			// draw the first part
			float i = -dist/2.0f;
			for( ; i < dist/2.0f; i += delta ) {
				pos_var[ 2 ] = -(m - k*i*i);
				line.setCoordinate( num++, pos_var );
				pos_var[ 0 ] = pos_var[ 0 ] + dx;
				pos_var[ 1 ] = pos_var[ 1 ] + dy;
			}
			line.setCoordinate( num++, pos_dest );

			line.setStripVertexCounts( new int[] { num } );
		} catch( Exception e ) {
			LOG.debug( "(LookUpLines)" + e.getMessage() );
			e.printStackTrace();
		}
	}
}
