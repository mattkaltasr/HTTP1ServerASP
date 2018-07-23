#!/usr/bin/php
<?php
//session_start();
$name;
$pass;
$pdcts = array("p1","p2","p3","p4","p5","p6");
$price = array(10,15,20,12,16,15);

    $cookies = array();

$products;
$cart;

if (defined('STDIN')) 
{
    if(isset($argv[1]))
    {
        $name = $argv[1];
    }
    
    if(isset($argv[2]))
    {    
        $pass = $argv[2];
    }
}
if(!empty($_SERVER['HTTP_COOKIE']))
{
    echo "cokiees = ";
    print_r($_SERVER['HTTP_COOKIE']);
    $cookie = explode(';', $_SERVER['HTTP_COOKIE']);
    //echo strlen($_SERVER['HTTP_COOKIE']);
    for($i =0; $i < count($cookie) ; $i = $i +1) {
        $cokie = $cookie[$i];
        echo $cokie."<br>";
        $parts = explode('=', $cokie);
        $xnm = trim($parts[0]);
        $len = strlen($xnm);
        echo $xnm."<br>";
//        if(substr($xnm, -1) === "=")
//        {
//            $xnm = substr($xnm, 0, $len-1);
//        }
//        echo $xnm."**".strlen($xnm);
        if(strlen($xnm) > 0)
        {
            if(empty($cookies[$xnm]))
            {
                $cookies[$xnm] = $parts[1];
            }
            else
            {
                $cookies[$xnm] = $cookies[$xnm].";".$parts[1];
            }
        }
        if(!empty($parts[1]))
        {
            echo $parts[1]."<br>";
        }
    }
    print_r($cookies);
//    $_SESSION['cookies'] = $cookies;
}
if($_SERVER['REQUEST_METHOD'] == 'POST')
{
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
            $name = $names[1];
            $passs = explode("=",$passPart);
            $pass = $passs[1];
            $products = $name;
            $cart = $pass;
        }
        else
        {
            $exp = explode("=", $postedDatas);
            $namePart = $exp[0];
            $valPart = $exp[1];
            if($namePart == "products")
            {
                $products = $namePart;
                unset($cart);
            }
            else
            {
                $cart = $namePart;
                unset($products);
            }
        }
    }
    else
    {
        unset($name);
        unset($pass);
    }
    //echo "got name = ".$name." and pass = ".$pass."<br>";
}
else
{
    echo "<script>console.log("."'get method'".");</script>";
    echo "<!DOCTYPE html>\n"
                . "<html>\n"
                . "<body>\n"
                . "\n"
                . "<form method=\"POST\" action=\"/cgi-bin/store.cgi\">\n"
                . "Name:<br>\n"
                . "<input type=\"text\" name=\"name\">\n"
                . "<br>\n"
                . "Password:<br>\n"
                . "<input type=\"password\" name=\"password\">\n"
                . "<br><br>\n"
                . "<input type=\"submit\" value=\"Submit\">\n"
                . "<input type=\"reset\">\n"
                . "</form>"
                . "\n"
                . "</body>"
                . "</html>";
    exit();
}

if (!isset($cookies['name']))
{
    
    if (isset($name) && isset($pass))
    {
        //setcookie("name", $name, time() + 180);
        //setcookie("cart", "", time() + (10 * 365 * 24 * 60 * 60));
        print_r($cookies);
        $cookies['cart'] = "";
        print_r($cookies);
        echo "<!DOCTYPE html>";
        
        echo "<html>"
                . "<head><title>CGI</title></head>\n";
        echo "<script>console.log("."'cookie not set'".");</script>";
        echo "<body>"
                . "<h1><strong>Hello ".$name."!</strong></h1>\n"
                . "<form method=\"POST\" action=\"/cgi-bin/store.cgi\">\n"
                . " <select name=\"products\">\n"
                . "     <option disabled selected value> -- select a product -- </option>\n";
        for($i =0; $i < count($pdcts); $i = $i + 1)
        {
            echo "  <option value=\"".$pdcts[$i]." ".$price[$i]."\">".$pdcts[$i]." ".$price[$i]."$"."</option>\n";
        }
        echo  "  </select>\n"
                . "  <input type=\"submit\" value=\"Add to Cart\">"
                . "</form> \n"
                . "\n"
                . "</body>\n"
                . "</html>";
        
    }
    else
    {
        //print_r($cookies);
        echo "<script>console.log("."'2 val not set'".");</script>";
        echo "<!DOCTYPE html>\n"
                . "<html>\n"
                . "<body>\n"
                . "\n"
                . "<form method=\"POST\" action=\"/cgi-bin/store.cgi\">\n"
                . "Name:<br>\n"
                . "<input type=\"text\" name=\"name\">\n"
                . "<br>\n"
                . "Password:<br>\n"
                . "<input type=\"password\" name=\"password\">\n"
                . "<br><br>\n"
                . "<input type=\"submit\" value=\"Submit\">\n"
                . "<input type=\"reset\">\n"
                . "</form>"
                . "\n"
                . "</body>"
                . "</html>";
    }
//    $_SESSION['cookies'] = $cookies;
    exit();
}
else
{
    echo "<script>console.log("."'cookie set'".");</script>";
    if((isset($products) || isset($cart)) && !empty($cookies['cart']))
    {
        echo "either one"."<br>";
        $exp;
        if(isset($cookies['cart']))
        {
            $arr = $cookies['cart'];
            $exp = explode(";", $arr);
        }
        print_r($exp);
        echo "<!DOCTYPE html>\n";
        echo "<html>\n"
                . "<head><title>CGI</title></head>\n";
        echo "<body>\n"
                . "\n"
                . "<h1><strong>Hello " . $cookies['name'] . "!</strong></h1>\n"
                . "<form method=\"POST\" action=\"/cgi-bin/store.cgi\">\n"
                . " <select name=\"products\">\n"
                . "<option disabled selected value> -- select a product to add -- </option>";
        $tot = 0;
        for($i =0; $i < count($pdcts); $i++)
        {
            $there = 0;
            $make = $pdcts[$i]."+".$price[$i];
            for($j = 0 ; $j < count($exp) ; $j++)
            {
                echo $exp[$j]."<br>".$make."<br>";
                if($exp[$j] == $make)
                {
                    $tot += $price[$j];
                    $there = 1;
                    break;
                }
            }
            if($there == 0)
            {
                echo "  <option value=\"".$pdcts[$i]." ".$price[$i]."\"> ".$pdcts[$i]." ".$price[$i]."$"."</option>\n";
            }
        }
        echo         "  </select>\n"
                . "  <input type=\"submit\" name=\"add\" value=\"Add to Cart\">"
                . " <select name=\"carted\">\n"
                . "<option disabled selected value> -- select a product to remove -- </option>";
                
            for($j = 0 ; $j < count($exp) ; $j++)
            {
                for($i=0;$i<count($pdcts);$i++)
                {
                    if($exp[$j]==$pdcts[$i])
                    {
                        echo "  <option value=\"".$pdcts[$i]." ".$price[$i]."\"> ".$pdcts[$i]." ".$price[$i]."$"."</option>\n";
                    }
                }
            }
            echo "  <option value=\"total\">total = ".$tot."$"."</option>\n";
        
        echo "  </select>\n"
                . "  <input type=\"submit\" name=\"remove\" value=\"Remove from Cart\">"
                . "</form> \n"
                . "\n"
                . "</body>\n"
                . "</html>";
    }
    else
    {
        echo "<!DOCTYPE html>\n";
        echo "<html>\n"
                . "<head><title>CGI</title></head>\n";
        echo "<body>\n"
                . "\n"
                . "<h1><strong>Hello " . $cookies['name'] . "!</strong></h1>\n"
                . "<form method=\"POST\" action=\"/cgi-bin/store.cgi\">\n"
                . " <select name=\"products\">\n"
                . "     <option disabled selected value> -- select a product -- </option>";
        for($i =0; $i < count($pdcts); $i++)
        {
            echo "  <option value=\"".$pdcts[$i]." ".$price[$i]."\"> ".$pdcts[$i]." ".$price[$i]."$"."</option>\n";
        }
        echo  "  </select>\n"
                . "  <input type=\"submit\" value=\"Add to Cart\">"
                . "</form> \n"
                . "\n"
                . "</body>\n"
                . "</html>";
    }
}
//echo 'Welcome ' . $name;
echo '</body> </html>';
?>
