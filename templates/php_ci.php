<?php

/**
 * CI model for ${remarks}.
 * Title: ${remarks}
 * User: ${user}
 * Date: ${date:yyyy-MM-dd HH:mm:ss}
 */
class ${name.prefix.pascal}_model extends CI_Model {

    public $table = '${name}';

    ${for:key=keys,inStr=",\n"}
    public ${name};
    ${endfor}
    
    public function __construct() {
    }

    // ${remarks} get detail
    public function get() {
        ${for:key=keys,inStr="\n"}
        $this->db->where(${item:key=name,padSize=15,quote='\''}, $_POST[${item:key=name,padSize=15,quote='\''}]);
        ${endfor}
        $query = $this->db->get($table);
        return $query->result();
    }

    // ${remarks} list
    public function getList() {
        $query = $this->db->get($table);
        return $query->result();
    }
    
    // ${remarks} input values
    public function getData() {
        return array(
            ${for:key=columns,inStr="\n,",indent=-1}
            ${item:key=name,quote='\'',padSize=20} => $_POST['${name}']
            ${endfor}
        );
    }

    // ${remarks} insert
    public function insert() {
        $data = getData();
        $this->db->insert($table, $data);
    }

    // ${remarks} modify
    public function update() {
        $data = getData();
        ${for:key=keys,inStr="\n"}
        $this->db->where(${item:key=name,padSize=15,quote='\''}, $_POST[${item:key=name,padSize=15,quote='\''}]);
        ${endfor}
        $this->db->update($table, data);
    }

    // ${remarks} delete
    public function delete() {
        $data = getData();
        ${for:key=keys,inStr="\n"}
        $this->db->where(${item:key=name,padSize=15,quote='\''}, $_POST[${item:key=name,padSize=15,quote='\''}]);
        ${endfor}
        $this->db->delete($table);
    }
}
