
$(function() {
	

// The root URL for the RESTful services	
var rootXoURL = "http://localhost:8080/JRWebflowXO/xoSession";
var rootMerchantURL = "http://localhost:8080/JRWebflowXO/merchant/";


//When launched in "page embedded" mode a flow will not issue flow execution redirects during Ajax requests.
//The mode=embedded parameter only needs to be passed when launching the flow. Your only
//other concern is to use Ajax requests and to render only the content required to update the portion of the
//page displaying the flow.
// /hotels/booking?mode=embedded

var merchantID = "9876";

var currentXoSession;

//var canvas = $("#myCanvas");
//because we�re using jQuery, we need to call the get method so we gain access to the DOM for the canvas element
//var context = canvas.get(0).getContext("2d");
//context.fillRect(40, 40, 100, 100);


// Retrieve XO Session list when application starts
//findAll();

// Nothing to delete or update in initial application state//$('#btnUpdate').hide();
//$('#btnDelete').hide();


$('#btnAdd').click(function(evt) {
  evt.preventDefault();	
  newXoSession(merchantID);
});


$('#btnUpdate').click(function(evt) {
  evt.preventDefault();	
  updateXoSession();
});

$('#btnSave').click(function(evt) {
  evt.preventDefault();	
  commitXoSession();
});


$('#btnDelete').click(function(evt) {
	evt.preventDefault();	
	deleteXoSession();
});

$.ajaxSetup({
    error: function(xhr){
        alert('Request Status: ' + xhr.status + ' Status Text: ' + xhr.statusText + ' ' + xhr.responseText);
    }
});



function newXoSession(merchantId) {
  $.ajax({
    url: rootMerchantURL + merchantId,
    type: "POST",
    dataType: "json",
   	success: function(data){
   		clearForm();
   		renderXoSession(data);
   	}
  });
}


function commitXoSession() {
  console.log('commitXoSession');
  $.ajax({
    type: 'POST',
    contentType: 'application/json',
    url: rootXoURL + '/' + $('#xoSessionId').val(),
    headers: {serviceVersion: 1},
    dataType: "json",
    data: formToJSON(),
    success: clearForm
  });
}

function updateXoSession() {
  console.log('updateXoSession');
  $.ajax({
    type: 'PUT',
    contentType: 'application/json',
    url: rootXoURL + '/' + $('#xoSessionId').val(),
    dataType: "json",
    data: formToJSON(),
    success: function(data){
    	renderXoSession(data);
    }
  });
}

function deleteXoSession() {
	// check for null XO session id
	if( $('#xoSessionId').val().length != 0 )
    {
		console.log('deleteXoSession');
		$.ajax({
		type: 'DELETE',
		url: rootXoURL + '/' + $('#xoSessionId').val(),
		success: clearForm
		});
    }
}



function clearForm() {
	$('#xoSessionId').val('');
	$('#merchant').val('');
	$('#buyer').val('');
	$('#item').val('');
	$('#price').val('');
	$('#purchaseDate').val('');
	$('#shippingAddress').val('');
}


function renderXoSession(xoSession) {
	console.log(xoSession);
	$('#xoSessionId').val(xoSession.xosessionId);
	$('#merchant').val(xoSession.merchant);
	$('#buyer').val(xoSession.buyer);
	$('#item').val(xoSession.item);
	$('#price').val(xoSession.price);
	$('#purchaseDate').datepicker("setDate", xoSession.purchaseDate);
	$('#shippingAddress').val(xoSession.shippingAddress);
}

// Helper function to serialize all the form fields into a JSON string
function formToJSON() {
	return JSON.stringify({
	"xosessionId": $('#xoSessionId').val(),
	"merchant": $('#merchant').val(),
	"buyer": $('#buyer').val(),
	"item": $('#item').val(),
	"price": $('#price').val(),
	"purchaseDate": $('#purchaseDate').datepicker("getDate"),
	"shippingAddress": $('#shippingAddress').val()
	});
}

});