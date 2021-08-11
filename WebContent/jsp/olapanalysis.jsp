<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<script src="/LPO_geoCartography/javascript/javascript.js"></script>
<script src="/LPO_geoCartography/javascript/w3.js"></script>

<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css">
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js"></script>


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
			<option selected="true" value="OAB">OAB</option>
			<option value="LPO">LPO</option>

		</select> <input type="submit" name="submit" value="Analyser" />
	</form>
		</section>
	
	
	<footer>
		<p></p>
	</footer>
</body>
</html>