
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
		var iframeOAB = '<iframe width="560" height="315" src="https://www.youtube.com/embed/3iwRtldZnM4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>'
		var iframeLPO = '<iframe width="560" height="315" src="https://www.youtube.com/embed/Z5PEZe9g75E" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>'
		
		if (id=='nav1') {
			document.getElementById("article").innerHTML =  "<div w3-include-HTML='/LPO_geoCartography/html/text1.html'></div>";
			w3.includeHTML();

		}  if (id=='nav2') {
			document.getElementById("article").innerHTML = iframeOAB;
			
			w3.includeHTML();

		}  if (id=='nav3') {
			document.getElementById("article").innerHTML = iframeLPO;
			w3.includeHTML();

		} 
			
		
	}

	function TraitementDonnees() {
		window.open("/LPO_geoCartography/TraitementDonnees");
	}

