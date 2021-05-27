
	/* Toggle between adding and removing the "responsive" class to topnav when the user clicks on the icon */
	function myTopnav() {
		var x = document.getElementById("myTopnav");
		if (x.className === "topnav") {
			x.className += " responsive";
		} else {
			x.className = "topnav";
		}
	}


	function nav(id) {
		if (id=='nav1') {
			document.getElementById("article").innerHTML =  "<div w3-include-HTML='/LPO_geoCartography/html/text1.html'></div>";
			w3.includeHTML();

		}  if (id=='nav2') {
			document.getElementById("article").innerHTML = "<img src='/LPO_geoCartography/images/OAB.png'/>";
			w3.includeHTML();

		}  if (id=='nav3') {
			document.getElementById("article").innerHTML = "<img src='/LPO_geoCartography/images/lpo.png'/>";
			w3.includeHTML();

		} 
			
		
	}

	function TraitementDonnees() {
		window.open("/LPO_geoCartography/TraitementDonnees");
	}

