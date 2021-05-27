var map, sld;
var format = new OpenLayers.Format.SLD();
var layerList;
var legende;
var legendes;
var titles;
var alr_bool;// j'utilise cette variable pour ne pas afficher la legende de même bar deux fois en mettant true pour la legende déjà affichée 
var Layer_selected;// j'utilise cette variable pour n'afficher dans la grande carte que les mesures ou indicateurs séléctionnés dans le multiMap

var styles;
var N;
var Layers;

function ChangeSize(ch) {
	for (var j=0; j<N; ++j) 
	{
	div_map = document.getElementById("map" +j);
	h = parseInt(div_map.style.height);
	w = parseInt(div_map.style.width);
	
	map1 = document.getElementById("map1" +j);
	h1 = parseInt(div_map.style.height);
	w1 = parseInt(div_map.style.width);

	if (ch)
		{div_map.setAttribute("style","float:left; width:"+(w+50)+"px; height:"+(h+25)+"px; border:double 5px black;");
			//map1.setAttribute("style","float:left; width:100%; height:"+(h1+25)+"px;");
		}
	else
		{div_map.setAttribute("style","float:left; width:"+Math.max(50,(w-50))+"px; height:"+Math.max(25,(h-25))+"px; border:double 5px black;");
			//map1.setAttribute("style","float:left; width:100%; height:"+Math.max(25,(h1-25))+"px;");

		
		}
	map[j].updateSize();
	}			
}

/*function fullScreen(theURL) {
	window.open(theURL, '', 'fullscreen=yes, scrollbars=yes,location=yes,resizable=yes');
	}*/
function init() {
//lire le fichier legende
	var text;
	var rawFile = new XMLHttpRequest();
    rawFile.open("GET", "legende.html", false);
    rawFile.onreadystatechange = function ()
    {
        if(rawFile.readyState === 4)
        {
            if(rawFile.status === 200 || rawFile.status == 0)
            {
                text = rawFile.responseText;
            }
        }
    }
    rawFile.send(null);
	legendes = text.split("<br><br>");
	
	//lire le fichier Titles
	var text_titles;
	var rawFile2 = new XMLHttpRequest();
	rawFile2.open("GET", "title.txt", false);
	rawFile2.onreadystatechange = function ()
    {
        if(rawFile2.readyState === 4)
        {
            if(rawFile2.status === 200 || rawFile2.status == 0)
            {
            	text_titles = rawFile2.responseText;
            }
        }
    }
	rawFile2.send(null);
	titles = text_titles.split("\n");
	
	//groupe_measures(titles[0])
	///////////////
	
	alr_bool = [];
	Layer_selected = [];
	layerList = [];
	map = [];
	
	var parameters = location.search.substring(1).split("&");

    var temp = parameters[0].split("=");
    N = unescape(temp[1]);
    
    var map_principale = document.getElementById("map");
    //map_principale.setAttribute("height","1000px");
    //map_principale.style.display = "none";
    //map_principale.style.display = "inline";
    //<img src="gras.png" onclick="javascript:tafonction();" alt="Texte en gras" style="cursor:pointer;" />
    if (N>1) // Multi Map
    {
    	btn_changeSize = document.createElement("img");
        //btn_changeSize.setAttribute("type","button");
        btn_changeSize.setAttribute("src","images/zoom in.jpg");
        //btn_changeSize.setAttribute("style","zoom in.jpg");
        btn_changeSize.setAttribute("style","cursor:pointer;");

        //btn_changeSize.setAttribute("value","<-- (+) -->");
        btn_changeSize.setAttribute("onclick","ChangeSize("+true+")");
        map_principale.appendChild(btn_changeSize);
        
        btn_changeSize = document.createElement("img");
        //btn_changeSize.setAttribute("type","button");
        //btn_changeSize.setAttribute("value","<-- (-) -->");
        btn_changeSize.setAttribute("src","images/zoom out.jpg");
        btn_changeSize.setAttribute("style","cursor:pointer;");

        btn_changeSize.setAttribute("onclick","ChangeSize("+false+")");
        //btn_changeSize.setAttribute("style","float:left;");
        map_principale.appendChild(btn_changeSize);
        
        br1 = document.createElement("br");
        br2 = document.createElement("br");
        map_principale.appendChild(br1);
        map_principale.appendChild(br2);	
    }
    

    
	for (var j=0; j<N; ++j) {
		
		//le conteneur de carte avec le bottun de chaque carte
		div_map = document.createElement("div");
		div_map.setAttribute("id","map" +j);
		div_map.setAttribute("class","smallmap");
		//div_map.setAttribute("style","float:left; width:200px; height:100px; border:solid 1px black;");
		if (N>1)	//MultisMap
			div_map.setAttribute("style","float:left; width:400px; height:200px; border:double 5px black; margin-left: auto; margin-right: auto;");
		else		//One Map
			div_map.setAttribute("style","float:left; width:100%; height:100%; border:double 5px black; margin-left: auto; margin-right: auto;");

		map_principale.appendChild(div_map);
		
		//Div de la carte
		var map1 = document.createElement("div");
		map1.setAttribute("id","map1" +j);
		map1.setAttribute("class","smallmap");
		if (N>1)
			map1.setAttribute("style","float:none; width:99%; height:90%; margin-left: auto; margin-right: auto;");
		else
			map1.setAttribute("style","float:none; width:100%; height:100%; margin-left: auto; margin-right: auto;");
		
		div_map.appendChild(map1);
		var map11 = document.getElementById("map1"+j);
		map[j] = new OpenLayers.Map(map11, {allOverlays: true});	
		var Layer0 = new OpenLayers.Layer.WMS( "OpenLayers WMS", "http://vmap0.tiles.osgeo.org/wms/vmap0", {layers: 'basic'} );
		map[j].addLayer(Layer0);
		layerList[j] = new Array();
		
		//div de title
		var title_div = document.createElement("div");
		title_div.setAttribute("id","title_div" +j);
		title_div.setAttribute("class","smallmap");
		
		if (N>1)
		{title_div.setAttribute("style","position:relative; top:-2px; float:none; width:99%; height:10%; margin-left: auto; margin-right: auto;");
		title_div.setAttribute("align", "center");


		div_map.appendChild(title_div);

		var title_link = document.createElement("a");
		title_link.setAttribute("id","title_link" +j);

		title_link.setAttribute("style","float:none;");
		//title_link.setAttribute("align", "center");

		//title_link.style.textAlign = "center";
		title_link.innerHTML=titles[j];
		title_link.style.fontSize = "12px";
		//title_link.href = 'AffichageCarte.html?N='+j;
		title_link.href = 'AffichageCarte.html?N='+j+'&Title='+titles[j];//+'&Layers='+Layers;//displaied_Layers(alr_bool);
		//title_link.onclick='fullScreen("http://www.w3schools.com");';
		//title_link.setAttribute('onclick','fullScreen("AffichageCarte.html?N="+j)');
		/*title_link.onClick=function () {
			window.open(theURL, "AffichageCarte.html?N="+j, 'fullscreen=yes', 'scrollbars=yes','location=yes','resizable=yes');
           };*/
		title_link.setAttribute("target", "_blank");
		
		title_div.appendChild(title_link);
		if (groupe_measures(titles[j])!= groupe_measures(titles[j+1]))
			{
	        br1 = document.createElement("br");
	        br1.setAttribute("style","clear:both;");
	        
	        br2 = document.createElement("br");
	        br2.setAttribute("style","clear:both;");

	        hr = document.createElement("hr");	       
	        hr.setAttribute("color","red");

	        map_principale.appendChild(br1);
	        map_principale.appendChild(hr);
	        //map_principale.appendChild(br2);
			}
		}
	}
    //var layerName = "Simple point";
	//var layerURL = "GML.xml";
	
	/*var layer = new OpenLayers.Layer.Vector(layerName, {
			protocol: new OpenLayers.Protocol.HTTP({
				url: layerURL,
				format: new OpenLayers.Format.GML()
			}),
			strategies: [new OpenLayers.Strategy.Fixed()], 
			styleMap: new OpenLayers.StyleMap()
		})
    map.addLayer(layer);

    waterBodies = layer;
    map.addControl(new OpenLayers.Control.SelectFeature(
        layer, {hover: true, autoActivate: true}
    ));*/
    //map.addControl(new OpenLayers.Control.LayerSwitcher());

    OpenLayers.Request.GET({
        url: "SLD.xml",
        success: complete
    });
}

// handler for the OpenLayers.Request.GET function in the init method
function complete(req) {
    sld = format.read(req.responseXML || req.responseText);
	
	var layerName = "Simple point";
	var layerURL = "GML";
	
	var nb = sld.namedLayers["Simple point"].userStyles;
	for (var j=0; j<N; ++j) {
		for (var i=0; i<nb.length; ++i) {
			alr_bool[i] = false;//aucune legende n'est pas encore affichée
			Layer_selected[i] = false;// aucune couche (Layer) n'est pas encoreséléctionnée 
			layerList[j][i] = new OpenLayers.Layer.Vector(layerName, {
				protocol: new OpenLayers.Protocol.HTTP({
					url: layerURL+j+".xml",
					format: new OpenLayers.Format.GML()
				}),
				strategies: [new OpenLayers.Strategy.Fixed()], 
				styleMap: new OpenLayers.StyleMap()
			});
			map[j].addLayer(layerList[j][i]);
			layerList[j][i].visibility = false;

			map[j].addControl(new OpenLayers.Control.SelectFeature(
					layerList[j][i], {hover: true, autoActivate: true}
			));
		}
		layerList[j][0].visibility = true;
	}
	
	update_links(Layer_selected);
    buildStyleChooser();
    setLayerStyles();
	for (var j=0; j<N; ++j) {
		map[j].zoomToExtent(new OpenLayers.Bounds(-5,65,65,40));
	}
}


function setLayerStyles() {
    // set the default style for each layer from sld
	for (var j=0; j<N; ++j) {
		for (var l in sld.namedLayers) {
			var styles = sld.namedLayers[l].userStyles, style;
			for (var i=0,ii=styles.length; i<ii; ++i) {
				style = styles[i];
				if (style.isDefault) {
					map[j].getLayersByName(l)[0].styleMap.styles["default"] = style;
					//break;
				}

				layerList[j][i].styleMap.styles["default"] = sld.namedLayers["Simple point"].userStyles[i];
				layerList[j][i].redraw();

				layerList[j][i].styleMap.styles.select = sld.namedLayers["Simple point"].userStyles[i];
			}
		}
	}
    // select style for mouseover on WaterBodies objects
    //waterBodies.styleMap.styles.select = sld.namedLayers["Simple point"].userStyles[0];
}


// add a check box for each userStyle
function buildStyleChooser() {
	
    styles = sld.namedLayers["Simple point"].userStyles;
    var chooser = document.getElementById("style_chooser"), input, li, span;
    legende = document.getElementById("legende"), input, li, span;
    
    // on commence la boucle à partir de deuxième style parce que le premier style est le Background 
    for (var i=1,ii=styles.length; i<ii; ++i) {
        input = document.createElement("input");
        input.type = "checkbox";
        input.name = "style";
		input.id = "style" + i;
        input.value = i;
/*        input.checked = i == 0;*/
        input.onclick = function() { setStyle(this.value); };
        li = document.createElement("li");
        li.appendChild(input);
        li.appendChild(document.createTextNode(styles[i].title));
		//span = document.createElement("span");
		//span.innerHTML += legendes[i];

//		if ((styles[i].title.includes("_._")))
//			li.appendChild(span);
//		else
//			{
//			span.innerHTML += "<br>";
//			legende.appendChild(span);
//			}		
				
        chooser.appendChild(li);
    }
}

// set a new style when the radio button changes
function setStyle(index) {
    //waterBodies.styleMap.styles["default"] = sld.namedLayers["Simple point"].userStyles[index];
    // apply the new style of the features of the Water Bodies layer
    //waterBodies.redraw();
	
	legende = document.getElementById("legende");
	
	if(document.getElementById("style" + index).checked) {
		Layer_selected[index]=true;
		for (var j=0; j<N; ++j) {
			layerList[j][index].setVisibility(true);
		}
		/*if (!(styles[index].title.includes("_._")) && !(alr_bool[index]))*/
		if (!(alr_bool[index]))

			{alr_bool[index]=true;
			span = document.createElement("span");
			span.setAttribute("id","lspan" +index);
			//legendes[index-1] parce que le premier Layer est pour le background
			span.innerHTML += legendes[index-1]+"<br><br>";
			legende.appendChild(span);
			
			}
		else if ((alr_bool[index]))
			document.getElementById("lspan" +index).style.display = "inline";
		//layerList[index].redraw();
	}
	else {
		Layer_selected[index]=false;
		for (var j=0; j<N; ++j) {
			layerList[j][index].setVisibility(false);
			layerList[j][index].redraw();
		}
		if(document.getElementById("lspan" +index))
		document.getElementById("lspan" +index).style.display = "none";
	}
	
	update_links(Layer_selected);
}



//extraire l'ensemble de noms des mesures à partir de titre de la carte
function groupe_measures(title) {
	
    var temp = title.split(",");
//    window.alert("title ="+title);
//    window.alert("temp.size="+temp.length);
    //var temp2;
    var measures = " ";
    for (var i=0; i < temp.length; i++)
    	{
    	var temp2 = temp[i].split("[");

   		measures = measures + temp2[0];    
    	}
//    window.alert("measures="+measures);
    return measures; 
}




//trouver les couches déjà affiché
function update_links(Layer_selected) {	
	if (N!=1)
	{
		for (var i = 0; i<N; i++)
		{
			var str="";
			
			//on commence par le deuxiéme Layer parce que le premier est le Background
			for (var j=1; j < Layer_selected.length; j++)
			{
				str = str + Layer_selected[j];
				if (j != Layer_selected.length-1)
					str = str +","; 
			}
			link = document.getElementById("title_link" +i);
			link.setAttribute("href",'AffichageCarte.html?N='+i+'&Title='+titles[i]+'&Layers='+str);

			//title_link.setAttribute("id","title_link" +j);
			//title_link.href = 'AffichageCarte.html?N='+j+'&Title='+titles[j];//+'&Layers='+Layers;//displaied_Layers(alr_bool);
		}
		//window.alert("Layers="+str);
	}
}
