<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<script src="/LPO_geoCartography/javascript/javascript.js"></script>
<script src="/LPO_geoCartography/javascript/w3.js"></script>
<link rel="stylesheet" type="text/css"
	href="/LPO_geoCartography/css/style.css">

</head>
<body>
	<div w3-include-HTML='/LPO_geoCartography/html/nav.html'></div>
	<script type="text/javascript">
		w3.includeHTML();
	</script>
	<section>

	<form action="/LPO_geoCartography/Accueil" method="post" id="desc2">
		<label for="Cube">Choisir un Cube:</label> <select id="Cube"
			name="Cube">
			<option selected="true" disabled="disabled">Select an Option</option>
			<option value="OAB">OAB</option>
			<option value="LPO">LPO</option>

		</select> <input type="submit" name="submit" value="Saiku" />
	</form>
		</section>
	
	<button type="button" onclick="TraitementDonnees()">Saiku</button>
	<footer>
		<p>Footer</p>
	</footer>
</body>
</html>