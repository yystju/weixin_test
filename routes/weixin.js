var express = require('express');
var router = express.Router();

/* GET users listing. */
router.get('/', function(req, res, next) {
  var signature = req.query['signature'];
  var echostr = req.query['echostr'];
  var timestamp = req.query['timestamp'];
  var nonce = req.query['nonce'];
  
  console.log('signature : ' + signature);
  console.log('echostr : ' + echostr);
  console.log('timestamp : ' + timestamp);
  console.log('nonce : ' + nonce);
  
  res.send(echostr);
});

module.exports = router;
