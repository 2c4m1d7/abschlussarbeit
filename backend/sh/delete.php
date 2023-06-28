<?php
$url = $_SERVER['REQUEST_URI']; 

$url = trim($url, '/'); 

$parts = explode('/', $url)[1]; 
$dbname = $parts[1];
$port = $parts[2];
exec("redis-cli -p $port shutdown && rm -r /data/$dbname");



