<?php
if (isset($_POST['name'])) {
    $name = $_POST['name'];

    $host = 'localhost';

    $start = 49152;
    $end = 65535;
    $port = $start;
    for (; $port < $end; $port++) {
        $connection = @fsockopen($host, $port);
        if (is_resource($connection)) {
            fclose($connection);
        } else {
            break;
        }
    }

        exec("mkdir /data ; mkdir /data/$name");

        exec("bash start_redis.sh $port $name > /dev/null 2>&1 &");
        exec("echo");
    

        $response = array("port"=>$port, "dbName"=>$name);
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode($response);
        return;
}
echo "parameter not set";
