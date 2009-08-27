<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" encoding="iso-8859-1" indent="yes"/>


<xsl:template match="table_attributes/entry">
	<xsl:element name="attribute"> 
		<xsl:element name="name">
 	 		<xsl:value-of select="name" />
 	 	</xsl:element>
		<xsl:element name="value">
			<xsl:value-of select="value" />
 	 	</xsl:element>
 	</xsl:element>
 </xsl:template>


<xsl:template match="table_dataobjects/entry">
	<xsl:variable name="dataobject_rowid" select="@rowid" />
	<xsl:variable name="dataobject_id" select="id" />

	<xsl:element name="dataobject"> 
		<xsl:element name="rowid">
			<xsl:value-of select="$dataobject_rowid" />
		</xsl:element>
		<xsl:element name="id">
			<xsl:value-of select="$dataobject_id" />
		</xsl:element>
		<xsl:for-each select="xmlhdr/Haggle/Data/Thumbnail">
			<xsl:element name="Thumbnail">
				<xsl:value-of select="." />
			</xsl:element>
		</xsl:for-each>
		<xsl:for-each select="xmlhdr/Haggle/Forward/PRoPHET">
			<xsl:element name="attribute">
				<xsl:element name="name">PRoPHET</xsl:element>
				<xsl:element name="value">
					<xsl:value-of select="." />
				</xsl:element>
			</xsl:element>
		</xsl:for-each>
	
		<xsl:for-each select="/HaggleInfo/HaggleDump/table_map_dataobjects_to_attributes_via_rowid/entry[dataobject_rowid=$dataobject_rowid]">
			<xsl:variable name="attr_rowid" select="attr_rowid" />
			<xsl:for-each select="/HaggleInfo/HaggleDump/table_attributes/entry[@rowid=$attr_rowid]">
				<xsl:apply-templates select="." />
			</xsl:for-each>
		</xsl:for-each>
	</xsl:element>


	<xsl:for-each select="/HaggleInfo/HaggleDump/table_dataobjects/entry[@rowid > $dataobject_rowid]">
		<xsl:variable name="dataobject2_rowid" select="@rowid" />
		<xsl:variable name="dataobject2_id" select="id" />

		<xsl:element name="relation"> 
			<xsl:element name="dataobject">
				<xsl:element name="rowid">
					<xsl:value-of select="$dataobject_rowid" />
				</xsl:element>
				<xsl:element name="id">
					<xsl:value-of select="$dataobject_id" />
				</xsl:element>
				<xsl:element name="num_attrs">
					<xsl:value-of select="count(/HaggleInfo/HaggleDump/table_map_dataobjects_to_attributes_via_rowid/entry[dataobject_rowid=$dataobject_rowid])" />
				</xsl:element>
			</xsl:element>

			<xsl:element name="dataobject">
				<xsl:element name="rowid">
					<xsl:value-of select="$dataobject2_rowid" />
				</xsl:element>
				<xsl:element name="id">
					<xsl:value-of select="$dataobject2_id" />
				</xsl:element>
				<xsl:element name="num_attrs">
					<xsl:value-of select="count(/HaggleInfo/HaggleDump/table_map_dataobjects_to_attributes_via_rowid/entry[dataobject_rowid=$dataobject2_rowid])" />
				</xsl:element>
			</xsl:element>


			<xsl:for-each select="/HaggleInfo/HaggleDump/table_map_dataobjects_to_attributes_via_rowid/entry[dataobject_rowid=$dataobject_rowid]">
				<xsl:variable name="attr_rowid" select="attr_rowid" />
					<xsl:for-each select="/HaggleInfo/HaggleDump/table_map_dataobjects_to_attributes_via_rowid/entry[dataobject_rowid=$dataobject2_rowid and attr_rowid=$attr_rowid]">
						<xsl:element name="matching_attribute">
							<xsl:value-of select="attr_rowid" />
						</xsl:element>
				</xsl:for-each>
			</xsl:for-each>

		</xsl:element>

	</xsl:for-each>
	
</xsl:template>


<xsl:template match="table_nodes/entry">
	<xsl:variable name="node_rowid" select="@rowid" />
	<xsl:variable name="node_id" select="id" />

	<xsl:element name="node">
		<xsl:attribute name="rowid">
			<xsl:value-of select="$node_rowid" />
		</xsl:attribute>
		<xsl:attribute name="id">
			<xsl:value-of select="$node_id" />
		</xsl:attribute>
		
		<xsl:for-each select="/HaggleInfo/HaggleDump/table_map_nodes_to_attributes_via_rowid/entry[node_rowid=$node_rowid]">
			<xsl:variable name="attr_rowid" select="attr_rowid" />
			<xsl:for-each select="/HaggleInfo/HaggleDump/table_attributes/entry[@rowid=$attr_rowid]">
				<xsl:apply-templates select="." />
				
			</xsl:for-each>
		</xsl:for-each>
	</xsl:element>

	<xsl:for-each select="/HaggleInfo/HaggleDump/table_attributes/entry[name='NodeDescription' and value=$node_id]">

	<xsl:element name="map">
		<xsl:element name="node">
			<xsl:element name="rowid">
				<xsl:value-of select="$node_rowid" />
			</xsl:element>
		</xsl:element>

		<xsl:variable name="attr_rowid" select="@rowid" />
		<xsl:for-each select="/HaggleInfo/HaggleDump/table_map_dataobjects_to_attributes_via_rowid/entry[attr_rowid=$attr_rowid]">
			<xsl:variable name="do_rowid" select="dataobject_rowid" />
			<xsl:for-each select="/HaggleInfo/HaggleDump/table_dataobjects/entry[@rowid=$do_rowid]">
				<xsl:element name="dataobject">
					<xsl:element name="rowid">
						<xsl:value-of select="@rowid" />
					</xsl:element>
				</xsl:element>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:element>
	</xsl:for-each>
	
</xsl:template>

<xsl:template match="ThisNode/Haggle">
	<xsl:element name="dataobject"> 
		<xsl:element name="ThisNode">Yes</xsl:element>
		<xsl:element name="id">A</xsl:element>
		<xsl:element name="rowid">-1</xsl:element>
		<xsl:for-each select="/HaggleInfo/ThisNode/Haggle/Attr">
			<xsl:element name="attribute">
				<xsl:element name="name">
					<xsl:value-of select="@name" />
				</xsl:element>
				<xsl:element name="value">
					<xsl:value-of select="." />
				</xsl:element>
			</xsl:element>
		</xsl:for-each>
		<xsl:element name="xmlhdr">
			<xsl:template match="/HaggleInfo/ThisNode">
				<xsl:copy-of select="node()"/>
			</xsl:template>
		</xsl:element>
	</xsl:element>


	<xsl:for-each select="/HaggleInfo/HaggleDump/table_dataobjects/entry">
		<xsl:variable name="dataobject2_rowid" select="@rowid" />
		<xsl:variable name="dataobject2_id" select="id" />

		<xsl:element name="relation"> 
			<xsl:element name="dataobject">
				<xsl:element name="rowid">-1</xsl:element>
				<xsl:element name="id">A</xsl:element>
				<xsl:element name="num_attrs">
					<xsl:value-of select="count(/HaggleInfo/ThisNode/Haggle/Attr)" />
				</xsl:element>
			</xsl:element>

			<xsl:element name="dataobject">
				<xsl:element name="rowid">
					<xsl:value-of select="$dataobject2_rowid" />
				</xsl:element>
				<xsl:element name="id">
					<xsl:value-of select="$dataobject2_id" />
				</xsl:element>
				<xsl:element name="num_attrs">
					<xsl:value-of select="count(/HaggleInfo/HaggleDump/table_map_dataobjects_to_attributes_via_rowid/entry[dataobject_rowid=$dataobject2_rowid])" />
				</xsl:element>
			</xsl:element>

		

			<xsl:for-each select="/HaggleInfo/ThisNode/Haggle/Attr">
				<xsl:variable name="attr_name" select="@name" />
				<xsl:variable name="attr_value" select="." />
				<xsl:for-each select="/HaggleInfo/HaggleDump/table_attributes/entry[name=$attr_name and value=$attr_value]">
					<xsl:variable name="attr_rowid" select="@rowid" />
					<xsl:for-each select="/HaggleInfo/HaggleDump/table_map_dataobjects_to_attributes_via_rowid/entry[attr_rowid=$attr_rowid and dataobject_rowid=$dataobject2_rowid]">

						<xsl:element name="matching_attribute">
							<xsl:value-of select="$attr_rowid" />
						</xsl:element>
					
					</xsl:for-each>
				</xsl:for-each>
			</xsl:for-each>
		</xsl:element>
	</xsl:for-each>
</xsl:template>

<xsl:template match="RoutingData/Haggle">
	<xsl:element name="dataobject"> 
		<xsl:element name="id">B</xsl:element>
		<xsl:element name="rowid">-2</xsl:element>
		<xsl:for-each select="/HaggleInfo/RoutingData/Haggle/Attr">
			<xsl:element name="attribute">
				<xsl:element name="name">
					<xsl:value-of select="@name" />
				</xsl:element>
				<xsl:element name="value">
					<xsl:value-of select="." />
				</xsl:element>
			</xsl:element>
		</xsl:for-each>
		<xsl:for-each select="/HaggleInfo/RoutingData/Haggle/Forward/PRoPHET">
			<xsl:element name="attribute">
				<xsl:element name="name">PRoPHET</xsl:element>
				<xsl:element name="value">
					<xsl:value-of select="." />
				</xsl:element>
			</xsl:element>
		</xsl:for-each>
	</xsl:element>
</xsl:template>

<xsl:template match="NeighborInfo/Neighbor">
	<xsl:element name="neighbor"> 
		<xsl:value-of select="." />
	</xsl:element>
</xsl:template>

<xsl:template match="RoutingTable">
	<xsl:element name="routingtable"> 
		<xsl:for-each select="/HaggleInfo/RoutingTable/Vector">
			<xsl:element name="vector">
				<xsl:variable name="vec_name" select="@name" />
				<xsl:element name="name">
					<xsl:value-of select="@name" />
				</xsl:element>
				<xsl:for-each select="./Metric">
					<xsl:element name="metric">
						<xsl:element name="name">
							<xsl:value-of select="@name" />
						</xsl:element>
						<xsl:element name="value">
							<xsl:value-of select="." />
						</xsl:element>
					</xsl:element>
				</xsl:for-each>
			</xsl:element>
		</xsl:for-each>
	</xsl:element>
</xsl:template>

<xsl:template match="/">
	<xsl:element name="relationgraph">
	<xsl:apply-templates select="/HaggleInfo/HaggleDump/table_dataobjects"/> 
	<xsl:apply-templates select="/HaggleInfo/HaggleDump/table_nodes"/> 
	<xsl:apply-templates select="/HaggleInfo/ThisNode/Haggle"/> 
	<xsl:apply-templates select="/HaggleInfo/RoutingData/Haggle"/> 
	<xsl:apply-templates select="/HaggleInfo/NeighborInfo/Neighbor"/> 
	<xsl:apply-templates select="/HaggleInfo/RoutingTable"/> 
	</xsl:element>
</xsl:template>


</xsl:stylesheet>