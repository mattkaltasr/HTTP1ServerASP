#!/usr/bin/php
<?php
    if($_SERVER['REQUEST_METHOD'] == 'POST')
    {
        $file;
        $md5;
        $postedDatas;
    //echo "Entered\n";
    $len = $_SERVER['CONTENT_LENGTH'];
    //echo "col = ".$len."\n";
    $stdin = fopen('php://stdin', 'r');
    $read = fread($stdin,$len);
    //echo "input = ".$read."<br>";
    $postedDatas = ltrim($read);
    if(count($postedDatas))
    {
        if(strpos($postedDatas,"&") !== false)
        {
            $exp = explode("&", $postedDatas);
            $namePart = $exp[0];
            $passPart = $exp[1];
            $names = explode("=",$namePart);
            $file = $names[1];
            $passs = explode("=",$passPart);
            $md5 = $passs[1];
        }
       
    }
        echo "file = ".$file."<br>";
        $md5hash = md5_file($file);
        echo "md5 = ".$md5."<br>";
        echo "md5hash = ".$md5hash."<br>";
        if(strcmp($md5,$md5hash) == 0)
           {
                //echo "md5 hash match"."<br>";
                echo "<!DOCTYPE html><html><head><title>Match</title></head><body>MD5 Hash Match!!</body></html></body> </html>";

            }
else
{
//echo "md5 hash don't match"."<br>";
echo "<!DOCTYPE html><html><head><title>Mismatch</title></head><body>MD5 Hash mismatch!!!</body></html></body> </html>";

}
    }
    else
    {
    		//set enctype attribute in form tag per (4)
        echo "<!DOCTYPE html><html><head><title>Services</title></head><body><form method='POST' action='/cgi-bin/service.cgi' enctype='multipart/form-data'><input type='file' name='file'><input type='text' name='text'><input type='submit' name='submit' value='Submit'></form></body></html></body> </html>";
    }
?>
