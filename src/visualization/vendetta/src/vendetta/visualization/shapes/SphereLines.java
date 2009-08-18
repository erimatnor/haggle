package vendetta.vendettagui.shapes;
import javax.media.j3d.*;
import javax.vecmath.Vector3f;
import vendetta.Vendetta;
import vendetta.visualization.shapes.*;
import vendetta.util.log.Log;

public class SphereLines extends javax.media.j3d.BranchGroup {
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

	
	/** Creates a new lookup line, starting at startNode */
	public SphereLines( MonitorNodeShape startNode, String seq, String key,
			float[] col ) {
		super();

		this.seq = seq;
		this.key = key;
		
		tg = new TransformGroup();
		trans = new Transform3D();
		app = new Appearance();
		color = new ColoringAttributes( col[ 0 ], col[ 1 ], col[ 2 ],
			   ColoringAttributes.NICEST );
		addChild( tg );
		app.setColoringAttributes( color );
		setPickable( false );
		setCapability( ALLOW_DETACH );
		lines = new LineStripArray( MAX_P, GeometryArray.COORDINATES, strip_count );
		
		lines.setCapability( lines.ALLOW_COORDINATE_WRITE );
		lines.setCapability( lines.ALLOW_COUNT_WRITE );	
		//lines.setStripVertexCounts( new int[] { 2, 2 } );
		
		// Draw a line 
		float[] pos_src = startNode.getPosition();
		lines.setCoordinate( tot_num++, startNode.getPosition() );
		lines.setCoordinate( tot_num++, new float[] { 0.0f, 0.0f, 0.0f } );
		
		LineAttributes la = new LineAttributes();
		la.setLineAntialiasingEnable( true );
		la.setLineWidth( 1.0f );
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

	public void addLine( MonitorNodeShape src, MonitorNodeShape dest ) {
		try {
			if( src == null || dest == null ) {
				LOG.warn( "(LookUpLine)" + "Skipping a line because" +
					" the shape was not found." );
				return;
			}

			line_num++;
			int num = 0;

			float[] pos_src = src.getPosition();
			float[] pos_dest = dest.getPosition();
			
			float[] M = { 	pos_dest[ 0 ] - pos_src[ 0 ],
							pos_dest[ 1 ] - pos_src[ 1 ],
							pos_dest[ 2 ] - pos_src[ 2 ] };

			float dist = length( M );
			float[] L = normalize( M );
			
			float[] P = { 	pos_src[ 0 ] + L[ 0 ]*( dist/2.0f ),
							pos_src[ 1 ] + L[ 1 ]*( dist/2.0f ),
							pos_src[ 2 ] + L[ 2 ]*( dist/2.0f ) };
			
			//float m = 5.1f;
			//float k = m / ( ( dist/2.0f )*( dist/2.0f ) );
			float k = 0.09f;
			float m = (dist/2.0f)*(dist/2.0f)*k;
			
			//float[] H = normalize( P );
			float[] H = normalize( pos_src );
			
			float[] Pi = new float[ 3 ];
			Pi[ 0 ] = pos_src[ 0 ];
			Pi[ 1 ] = pos_src[ 1 ];
			Pi[ 2 ] = pos_src[ 2 ];
			
			int num_steps = (int)dist * 2;
			if( num_steps < 8 )
				num_steps = 8;
			
		//	vendetta.Vendetta.msg( "dist: " + dist + " num_steps: " + num_steps );
			
			float delta = dist / (float)num_steps;
			float n = 0.0f;

			if( tot_num + 1 >= MAX_P ) return;
			lines.setCoordinate( tot_num++, pos_src );
			num++;

			for( int i = 0; i < num_steps + 1; i++ ) {
					
				n = ( i*delta - dist/2.0f );
				n *= n;
				n *= k;
				for( int p = 0; p < 3; p++ ) {
					Pi[ p ] = pos_src[ p ] + L[ p ]*delta*i + H[ p ]*m - H[ p ] * n;
				}
				H = normalize( Pi );
								
				if( tot_num + 1 >= MAX_P ) return;
				lines.setCoordinate( tot_num++, Pi );
				num++;
			}
			
			lines.setCoordinate( tot_num++, pos_dest );
			num++;
			if( tot_num + 1 >= MAX_P ) return;

			// increase the strip count
			increase_stripcount();
			strip_count[ line_num ] = num;
			lines.setStripVertexCounts( strip_count );

		} catch( Exception e ) {
			LOG.debug( "(LookUpLines)" + e.getMessage() );
			e.printStackTrace();
		}
	}

	private float[] normalize( float[] V ) {
		// Calculate the length
		float length = (float)Math.sqrt( 
			V[ 0 ]*V[ 0 ] + V[ 1 ]*V[ 1 ] + V[ 2 ]*V[ 2 ] );
		return new float[] { V[ 0 ]/ length, V[ 1 ]/ length, V[ 2 ]/ length };
	}

	private float length( float[] V ) {
		return (float)Math.sqrt( V[ 0 ]*V[ 0 ] + V[ 1 ]*V[ 1 ] + V[ 2 ]*V[ 2 ] );
	}
}
