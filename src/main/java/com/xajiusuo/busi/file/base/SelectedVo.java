package com.xajiusuo.busi.file.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SelectedVo implements Serializable {

    private Integer id;

    private boolean selected;

    private boolean checked;

    private String label;

    private String value;

    private List<SelectedVo> children;


    public SelectedVo(){
        this.selected = false;
        this.checked =false;
        this.children = Collections.emptyList();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<SelectedVo> getChildren() {
        if(Collections.emptyList().equals(children)){
            children = new ArrayList<>();
        }
        return children;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectedVo that = (SelectedVo) o;
        return selected == that.selected &&
                checked == that.checked &&
                Objects.equals(id, that.id) &&
                Objects.equals(label, that.label) &&
                Objects.equals(value, that.value) &&
                Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, selected, checked, label, value, children);
    }

    @Override
    public String toString() {
        return "SelectedVo{" +
                "id=" + id +
                ", selected=" + selected +
                ", checked=" + checked +
                ", label='" + label + '\'' +
                ", value=" + value +
                ", children=" + children +
                '}';
    }
}
