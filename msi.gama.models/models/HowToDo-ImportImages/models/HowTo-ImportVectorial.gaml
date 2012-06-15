/**
 *  HowToImportRaster
 *  Author: Maroussia Vavasseur and Benoit Gaudou
 *  Description: Importation of a vectorial image
 */

model HowToImportVectorial



global {
	// Global variables  related to the Management units	
	file ManagementUnitShape <- file('../images/ug/UGSelect.shp'); 
	
	init {
		create managementUnit from: ManagementUnitShape.path 
			with: [MUcode::read('Code_UG'), MULabel::read('Libelle_UG'), pgeSAGE::read('PGE_SAGE')] ;
    }
}

environment bounds: ManagementUnitShape.path {}

entities {	
	species managementUnit{
		int MUcode;
		string MULabel;
		string pgeSAGE;
		
		aspect basic{
    		draw geometry: shape;
    	}
	}	
}

experiment main type: gui {
	parameter 'Management unit' var: ManagementUnitShape category: 'MU' ;
		
	output {
		display HowToImportVectorial {
	   		image name: 'GISBackground' gis: ManagementUnitShape.path color: rgb('blue');
	   		species managementUnit aspect: basic; 
		}
	    inspect name: 'Species' type: species refresh_every: 5;
	}
}


